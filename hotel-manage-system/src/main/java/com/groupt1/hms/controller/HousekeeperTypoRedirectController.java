package com.groupt1.hms.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Redirects common URL typos to the correct housekeeper routes.
 */
@Controller
public class HousekeeperTypoRedirectController {

    @GetMapping("/housekeper/tasks")
    public String tasksTypo() {
        return "redirect:/housekeeper/tasks";
    }

    @GetMapping("/housekeper/problems")
    public String problemsTypo() {
        return "redirect:/housekeeper/problems";
    }
}
