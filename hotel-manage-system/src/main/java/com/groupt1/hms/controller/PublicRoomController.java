package com.groupt1.hms.controller;

import com.groupt1.hms.entity.RoomType;
import com.groupt1.hms.service.ManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Public-facing Spring MVC controller that renders the hotel website's room
 * pages server-side from the shared {@code free-hotel-website-template} views.
 *
 * <p>Previously this was a {@code @RestController} exposing {@code /api/public/**}
 * JSON consumed by {@code public-rooms.js}. It now returns Thymeleaf view names
 * and populates the model directly, so the room cards and details are rendered
 * on the server.
 */
@Controller
public class PublicRoomController {

    @Autowired
    private ManagerService managerService;

    // ──────────────── Home page (featured rooms) ────────────────

    @GetMapping({"/", "/index.html"})
    public String index(Model model) {
        List<RoomType> roomTypes = managerService.getAllRoomTypes();
        // The home page only highlights the first four room types.
        List<RoomType> featured = roomTypes.size() > 4 ? roomTypes.subList(0, 4) : roomTypes;
        model.addAttribute("featuredRoomTypes", featured);
        model.addAttribute("activePage", "home");
        return "index";
    }

    // ──────────────── Rooms listing ────────────────

    @GetMapping("/rooms.html")
    public String rooms(Model model) {
        model.addAttribute("roomTypes", managerService.getAllRoomTypes());
        model.addAttribute("activePage", "rooms");
        return "rooms";
    }

    // ──────────────── Static Pages ────────────────

    @GetMapping("/restaurant.html")
    public String restaurant(Model model) {
        model.addAttribute("activePage", "restaurant");
        return "restaurant";
    }

    @GetMapping("/about.html")
    public String about(Model model) {
        model.addAttribute("activePage", "about");
        return "about";
    }

    @GetMapping("/contact.html")
    public String contact(Model model) {
        model.addAttribute("activePage", "contact");
        return "contact";
    }

    @GetMapping("/blog.html")
    public String blog(Model model) {
        model.addAttribute("activePage", "blog");
        return "blog";
    }

    @GetMapping("/blog-single.html")
    public String blogSingle(Model model) {
        model.addAttribute("activePage", "blog");
        return "blog-single";
    }

    // ──────────────── Single room details ────────────────

    @GetMapping("/rooms-single.html")
    public String roomDetails(@RequestParam(value = "id", required = false) Integer id, Model model) {
        if (id == null) {
            return "redirect:/rooms.html";
        }

        return managerService.getRoomTypeById(id)
                .map(roomType -> {
                    model.addAttribute("roomType", roomType);
                    model.addAttribute("activePage", "rooms");
                    return "rooms-single";
                })
                .orElse("redirect:/rooms.html");
    }
}
