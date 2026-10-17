package com.college.lostfound.config;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.ItemType;
import com.college.lostfound.model.User;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/** Creates a demo user and two sample items the first time the app runs. */
@Component
public class DataSeeder implements CommandLineRunner {

    private final UserRepository users;
    private final ItemRepository items;
    private final PasswordEncoder encoder;

    public DataSeeder(UserRepository users, ItemRepository items, PasswordEncoder encoder) {
        this.users = users;
        this.items = items;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) {
            return;
        }
        User demo = new User();
        demo.setName("Demo Student");
        demo.setEmail("demo@college.com");
        demo.setPasswordHash(encoder.encode("demo123"));
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
