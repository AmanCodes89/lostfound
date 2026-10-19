package com.college.lostfound.controller;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.ItemStatus;
import com.college.lostfound.model.ItemType;
import com.college.lostfound.model.User;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
public class ItemController {

    private static final List<String> CATEGORIES = List.of(
            "Phone", "Wallet", "Keys", "Bag", "Books", "ID Card",
            "Documents", "Electronics", "Clothing", "Other");

    private final ItemRepository items;
    private final UserRepository users;

    public ItemController(ItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    /** Logged-in user from the session (null if not logged in). */
    private User currentUser(HttpSession session) {
        Object o = session.getAttribute("userId");
        if (o instanceof Long id) {
            return users.findById(id).orElse(null);
        }
        return null;
    }

    /** Owner of the item, or the admin, may change it. Nobody else. */
    private boolean canManage(User user, Item item) {
        if (user == null) {
            return false;
        }
        if (user.isAdmin()) {
            return true;
        }
        return item.getPostedBy() != null && user.getId().equals(item.getPostedBy().getId());
    }

    private boolean has(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    @GetMapping("/items")
    public String list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String q,
                       Model model) {
        String query = (q == null) ? "" : q.trim().toLowerCase();

        List<Item> list = items.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> type == null || type.isBlank() || i.getType().name().equalsIgnoreCase(type))
                .filter(i -> status == null || status.isBlank()
                        || (i.getStatus() != null && i.getStatus().name().equalsIgnoreCase(status)))
                .filter(i -> query.isEmpty()
                        || has(i.getTitle(), query)
                        || has(i.getLocation(), query)
                        || has(i.getCategory(), query)
                        || has(i.getDescription(), query))
                .toList();

        model.addAttribute("items", list);
        model.addAttribute("statuses", ItemStatus.values());
        model.addAttribute("type", type == null ? "" : type);
        model.addAttribute("status", status == null ? "" : status);
        model.addAttribute("q", q == null ? "" : q);
        return "items";
    }

    @GetMapping("/items/new")
    public String newForm(@RequestParam(required = false) String type,
                          @RequestParam(required = false) String category,
                          HttpSession session, Model model, RedirectAttributes ra) {
        if (currentUser(session) == null) {
            ra.addFlashAttribute("error", "Please login to register a complaint.");
            return "redirect:/login";
        }
        model.addAttribute("today", LocalDate.now().toString());
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("type", "FOUND".equalsIgnoreCase(type) ? "FOUND" : "LOST");
        model.addAttribute("category", category == null ? "Other" : category);
        return "item_form";
    }

    @PostMapping("/items")
    public String create(@RequestParam String title,
                         @RequestParam(defaultValue = "") String description,
                         @RequestParam String location,
                         @RequestParam String category,
                         @RequestParam String type,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
                         @RequestParam(defaultValue = "") String contact,
                         HttpSession session,
                         RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }
        if (title.isBlank() || location.isBlank()) {
            ra.addFlashAttribute("error", "Title and location are required.");
            return "redirect:/items/new";
        }

        Item item = new Item();
        item.setTitle(title.trim());
        item.setDescription(description.trim());
        item.setLocation(location.trim());
        item.setCategory(category);
        item.setEventDate(eventDate);
        item.setContact(contact.trim());
        item.setType("FOUND".equalsIgnoreCase(type) ? ItemType.FOUND : ItemType.LOST);
        item.setPostedBy(user);
        items.save(item);

        ra.addFlashAttribute("msg", "Complaint registered successfully.");
        return "redirect:/items";
    }

    @GetMapping("/items/{id}")
    public String detail(@PathVariable Long id,
                         HttpSession session,
                         Model model,
                         RedirectAttributes ra) {
        Optional<Item> opt = items.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Item not found.");
            return "redirect:/items";
        }
        Item item = opt.get();
        User user = currentUser(session);

        model.addAttribute("item", item);
        model.addAttribute("loggedIn", user != null);
        model.addAttribute("admin", user != null && user.isAdmin());
        model.addAttribute("canManage", canManage(user, item));
        model.addAttribute("statuses", ItemStatus.values());
        return "detail";
    }

    @GetMapping("/items/{id}/edit")
    public String editForm(@PathVariable Long id,
                           HttpSession session,
                           Model model,
                           RedirectAttributes ra) {
        Optional<Item> opt = items.findById(id);
        if (opt.isEmpty() || !canManage(currentUser(session), opt.get())) {
            ra.addFlashAttribute("error", "You can only edit your own complaint.");
            return "redirect:/items";
        }
        model.addAttribute("item", opt.get());
        model.addAttribute("categories", CATEGORIES);
        return "edit_form";
    }

    @PostMapping("/items/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam String title,
                         @RequestParam(defaultValue = "") String description,
                         @RequestParam String location,
                         @RequestParam String category,
                         @RequestParam String type,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate eventDate,
                         @RequestParam(defaultValue = "") String contact,
                         HttpSession session,
                         RedirectAttributes ra) {
        Optional<Item> opt = items.findById(id);
        if (opt.isEmpty() || !canManage(currentUser(session), opt.get())) {
            ra.addFlashAttribute("error", "You can only edit your own complaint.");
            return "redirect:/items";
        }
        if (title.isBlank() || location.isBlank()) {
            ra.addFlashAttribute("error", "Title and location are required.");
            return "redirect:/items/" + id + "/edit";
        }
        Item item = opt.get();
        item.setTitle(title.trim());
        item.setDescription(description.trim());
        item.setLocation(location.trim());
        item.setCategory(category);
        item.setEventDate(eventDate);
        item.setContact(contact.trim());
        item.setType("FOUND".equalsIgnoreCase(type) ? ItemType.FOUND : ItemType.LOST);
        item.setUpdatedAt(LocalDateTime.now());
        items.save(item);

        ra.addFlashAttribute("msg", "Complaint updated.");
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{id}/resolve")
    public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Optional<Item> opt = items.findById(id);
        if (opt.isPresent() && canManage(currentUser(session), opt.get())) {
            Item item = opt.get();
            item.setStatus(ItemStatus.RESOLVED);
            item.setUpdatedAt(LocalDateTime.now());
            items.save(item);
            ra.addFlashAttribute("msg", "Marked as resolved.");
        } else {
            ra.addFlashAttribute("error", "You can only change your own complaint.");
        }
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        User user = currentUser(session);
        Optional<Item> opt = items.findById(id);
        if (opt.isPresent() && canManage(user, opt.get())) {
            items.delete(opt.get());
            ra.addFlashAttribute("msg", "Complaint deleted.");
            return user.isAdmin() ? "redirect:/admin" : "redirect:/items";
        }
        ra.addFlashAttribute("error", "You can only delete your own complaint.");
        return "redirect:/items";
    }
}
