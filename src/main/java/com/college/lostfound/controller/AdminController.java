package com.college.lostfound.controller;

import com.college.lostfound.model.Item;
import com.college.lostfound.model.ItemStatus;
import com.college.lostfound.model.User;
import com.college.lostfound.repository.ItemRepository;
import com.college.lostfound.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/** Dean office panel. Every method re-checks the admin role from the database. */
@Controller
public class AdminController {

    private final ItemRepository items;
    private final UserRepository users;

    public AdminController(ItemRepository items, UserRepository users) {
        this.items = items;
        this.users = users;
    }

    private User currentUser(HttpSession session) {
        Object o = session.getAttribute("userId");
        if (o instanceof Long id) {
            return users.findById(id).orElse(null);
        }
        return null;
    }

    private long count(List<Item> all, ItemStatus s) {
        return all.stream().filter(i -> i.getStatus() == s).count();
    }

    @GetMapping("/admin")
    public String dashboard(HttpSession session, Model model, RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null) {
            ra.addFlashAttribute("error", "Please login as Dean office.");
            return "redirect:/login";
        }
        if (!user.isAdmin()) {
            ra.addFlashAttribute("error", "Only Dean office (admin) can open this page.");
            return "redirect:/";
        }

        List<Item> all = items.findAllByOrderByCreatedAtDesc();
        model.addAttribute("items", all);
        model.addAttribute("users", users.findAll());
        model.addAttribute("statuses", ItemStatus.values());
        model.addAttribute("total", all.size());
        model.addAttribute("openCount", count(all, ItemStatus.OPEN));
        model.addAttribute("reviewCount", count(all, ItemStatus.UNDER_REVIEW));
        model.addAttribute("verifiedCount", count(all, ItemStatus.VERIFIED));
        model.addAttribute("resolvedCount", count(all, ItemStatus.RESOLVED));
        model.addAttribute("rejectedCount", count(all, ItemStatus.REJECTED));
        return "admin";
    }

    @PostMapping("/admin/items/{id}/status")
    public String updateStatus(@PathVariable Long id,
                               @RequestParam String status,
                               @RequestParam(defaultValue = "") String note,
                               @RequestParam(defaultValue = "admin") String from,
                               HttpSession session,
                               RedirectAttributes ra) {
        User user = currentUser(session);
        if (user == null || !user.isAdmin()) {
            ra.addFlashAttribute("error", "Only Dean office (admin) can change status.");
            return "redirect:/";
        }
        Optional<Item> opt = items.findById(id);
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "Item not found.");
            return "redirect:/admin";
        }
        ItemStatus newStatus;
        try {
            newStatus = ItemStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", "Invalid status.");
            return "redirect:/admin";
        }

        String cleanNote = note.trim();
        if (cleanNote.length() > 500) {
            cleanNote = cleanNote.substring(0, 500);
        }

        Item item = opt.get();
        item.setStatus(newStatus);
        item.setAdminNote(cleanNote.isEmpty() ? null : cleanNote);
        item.setUpdatedAt(LocalDateTime.now());
        items.save(item);

        ra.addFlashAttribute("msg", "Status updated to " + newStatus.getLabel() + ".");
        return "detail".equals(from) ? "redirect:/items/" + id : "redirect:/admin";
    }
}
