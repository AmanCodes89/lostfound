package com.college.lostfound.controller;

import com.college.lostfound.model.User;
import com.college.lostfound.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;

    public AuthController(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           RedirectAttributes ra) {
        name = name.trim();
        email = email.trim().toLowerCase();

        if (name.isEmpty() || email.isEmpty() || password.length() < 4) {
            ra.addFlashAttribute("error", "Fill all fields (password minimum 4 characters).");
            return "redirect:/register";
        }
        if (users.findByEmail(email).isPresent()) {
            ra.addFlashAttribute("error", "This email is already registered.");
            return "redirect:/register";
        }
        User u = new User();
        u.setName(name);
        u.setEmail(email);
        u.setPasswordHash(encoder.encode(password));
        users.save(u);

        ra.addFlashAttribute("msg", "Registration successful. Please login.");
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email,
                        @RequestParam String password,
                        HttpSession session,
                        RedirectAttributes ra) {
        Optional<User> found = users.findByEmail(email.trim().toLowerCase());
        if (found.isEmpty() || !encoder.matches(password, found.get().getPasswordHash())) {
            ra.addFlashAttribute("error", "Wrong email or password.");
            return "redirect:/login";
        }
        session.setAttribute("userId", found.get().getId());
        session.setAttribute("userName", found.get().getName());
        session.setAttribute("role", found.get().isAdmin() ? "ADMIN" : "STUDENT");
        return found.get().isAdmin() ? "redirect:/admin" : "redirect:/";
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }
}
