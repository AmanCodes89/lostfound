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
import java.util.List;
import java.util.Optional;

@Controller
public class ItemController {

    private final ItemRepository items;
    private final UserRepository users;

    public ItemController(ItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    private Long currentUserId(HttpSession session) {
        Object o = session.getAttribute("userId");
        return (o instanceof Long) ? (Long) o : null;
    }

    private boolean has(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    @GetMapping("/items")
    public String list(@RequestParam(required = false) String type,
                       @RequestParam(required = false) String q,
                       Model model) {
        String query = (q == null) ? "" : q.trim().toLowerCase();

        List<Item> list = items.findAllByOrderByCreatedAtDesc().stream()
                .filter(i -> type == null || type.isBlank() || i.getType().name().equalsIgnoreCase(type))
                .filter(i -> query.isEmpty()
                        || has(i.getTitle(), query)
                        || has(i.getLocation(), query)
                        || has(i.getCategory(), query)
                        || has(i.getDescription(), query))
                .toList();

        model.addAttribute("items", list);
        model.addAttribute("type", type == null ? "" : type);
        model.addAttribute("q", q == null ? "" : q);
        return "items";
    }

    private static final List<String> CATEGORIES = List.of(
            "Phone", "Wallet", "Keys", "Bag", "Books", "ID Card",
            "Documents", "Electronics", "Clothing", "Other");

    @GetMapping("/items/new")
    public String newForm(@RequestParam(required = false) String type,
                          @RequestParam(required = false) String category,
                          HttpSession session, Model model, RedirectAttributes ra) {
        if (currentUserId(session) == null) {
            ra.addFlashAttribute("error", "Please login to post an item.");
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
        Long uid = currentUserId(session);
        if (uid == null) {
            ra.addFlashAttribute("error", "Please login first.");
            return "redirect:/login";
        }
        Optional<User> user = users.findById(uid);
        if (user.isEmpty()) {
            session.invalidate();
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
        item.setPostedBy(user.get());
        items.save(item);

        ra.addFlashAttribute("msg", "Item posted successfully.");
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
        Long uid = currentUserId(session);
        boolean canManage = uid != null && item.getPostedBy() != null
                && uid.equals(item.getPostedBy().getId());

        model.addAttribute("item", item);
        model.addAttribute("loggedIn", uid != null);
        model.addAttribute("canManage", canManage);
        return "detail";
    }

    @PostMapping("/items/{id}/resolve")
    public String resolve(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long uid = currentUserId(session);
        Optional<Item> opt = items.findById(id);
        if (opt.isPresent() && uid != null && opt.get().getPostedBy() != null
                && uid.equals(opt.get().getPostedBy().getId())) {
            Item item = opt.get();
            item.setStatus(ItemStatus.RESOLVED);
            items.save(item);
            ra.addFlashAttribute("msg", "Marked as resolved.");
        }
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        Long uid = currentUserId(session);
        Optional<Item> opt = items.findById(id);
        if (opt.isPresent() && uid != null && opt.get().getPostedBy() != null
                && uid.equals(opt.get().getPostedBy().getId())) {
            items.delete(opt.get());
            ra.addFlashAttribute("msg", "Item deleted.");
        }
        return "redirect:/items";
    }
}
