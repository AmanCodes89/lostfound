package com.college.lostfound.config;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.ItemType;
import com.college.lostfound.model.User;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

/** Creates the Dean office admin account, and (first run only) a demo student + sample items. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final ItemRepository items;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:dean@college.com}")
    private String adminEmail;

    @Value("${app.admin.password:dean123}")
    private String adminPassword;

    @Value("${app.admin.name:Dean Office}")
    private String adminName;

    public DataSeeder(UserRepository users, ItemRepository items, PasswordEncoder encoder) {
        this.users = users;
        this.items = items;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        boolean firstRun = users.count() == 0;

        // 1. Admin (Dean office) account
        String email = adminEmail.trim().toLowerCase();
        Optional<User> existing = users.findByEmail(email);
        if (existing.isEmpty()) {
            User admin = new User();
            admin.setName(adminName);
            admin.setEmail(email);
            admin.setPasswordHash(encoder.encode(adminPassword));
            admin.setRole("ADMIN");
            users.save(admin);
        } else if (!existing.get().isAdmin()) {
            existing.get().setRole("ADMIN");
            users.save(existing.get());
        }

        // 2. Demo student + sample items (only on a fresh database)
        if (!firstRun) {
            return;
        }
        User demo = new User();
        demo.setName("Demo Student");
        demo.setEmail("demo@college.com");
        demo.setPasswordHash(encoder.encode("demo123"));
        demo.setRole("STUDENT");
        users.save(demo);

        Item a = new Item();
        a.setTitle("Black Wallet");
        a.setDescription("Black leather wallet with college ID inside.");
        a.setCategory("Wallet");
        a.setLocation("Library");
        a.setEventDate(LocalDate.now().minusDays(1));
        a.setContact("demo@college.com");
        a.setType(ItemType.LOST);
        a.setPostedBy(demo);
        items.save(a);

        Item b = new Item();
        b.setTitle("Blue Water Bottle");
        b.setDescription("Found near the canteen, left on a table.");
        b.setCategory("Other");
        b.setLocation("Canteen");
        b.setEventDate(LocalDate.now());
        b.setContact("demo@college.com");
        b.setType(ItemType.FOUND);
        b.setPostedBy(demo);
        items.save(b);
    }
}
