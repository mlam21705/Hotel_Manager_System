package com.groupt1.hms.controller;

import com.groupt1.hms.entity.CleaningTask;
import com.groupt1.hms.entity.MaintenanceReport;
import com.groupt1.hms.entity.Room;
import com.groupt1.hms.service.HousekeeperService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/housekeeper")
public class HousekeeperViewController {


    @Autowired
    private HousekeeperService housekeeperService;

    /**
     * Checks if the session holds a logged-in user with the Housekeeper role.
     */
    private boolean isHousekeeper(HttpSession session) {
        if (session == null) return false;
        Object userObj = session.getAttribute("user");
        if (userObj instanceof Map) {
            Map<?, ?> userMap = (Map<?, ?>) userObj;
            Object role = userMap.get("role");
            return role instanceof String && "Housekeeper".equalsIgnoreCase((String) role);
        }
        return false;
    }

    private String getLoggedInUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    @GetMapping({"", "/"})
    public String indexRedirect(HttpSession session) {
        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }
        return "redirect:/housekeeper/home";
    }

    @GetMapping("/home")
    public String housekeeperHome(HttpSession session, Model model) {
        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }
        model.addAttribute("activePage", "home");
        return "housekeeper/housekeeper-home";
    }

    @GetMapping("/tasks")
    public String tasksView(
            @RequestParam(value = "date", required = false) String dateStr,
            @RequestParam(value = "status", required = false, defaultValue = "ALL") String status,
            HttpSession session,
            Model model) {

        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }

        String username = getLoggedInUsername();
        LocalDate date = null;
        if (dateStr != null && !dateStr.trim().isEmpty()) {
            try {
                date = LocalDate.parse(dateStr.trim());
            } catch (Exception e) {
                // Invalid format, keep as null
            }
        } else if (dateStr == null) {
            // Default to today on initial page load
            date = LocalDate.now();
        }

        List<CleaningTask> tasks = housekeeperService.getAssignedTasks(username, date, status);

        model.addAttribute("tasks", tasks);
        model.addAttribute("selectedDate", date != null ? date.toString() : "");
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activePage", "tasks");

        return "housekeeper/tasks";
    }

    @PostMapping("/tasks/update-status")
    public String updateTaskStatus(
            @RequestParam("taskId") Integer taskId,
            @RequestParam("taskStatus") String taskStatus,
            @RequestParam("roomStatus") String roomStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }

        String username = getLoggedInUsername();
        try {
            housekeeperService.updateCleaningStatus(taskId, taskStatus, roomStatus, username);
            redirectAttributes.addFlashAttribute("successMessage", "Task updated successfully.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error updating task: " + e.getMessage());
        }

        return "redirect:/housekeeper/tasks";
    }

    @GetMapping("/problems")
    public String problemsView(HttpSession session, Model model) {
        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }

        String username = getLoggedInUsername();
        List<MaintenanceReport> problems = housekeeperService.getReportedProblems(username);
        List<Room> assignedRooms = housekeeperService.getAllRooms();

        model.addAttribute("problems", problems);
        model.addAttribute("assignedRooms", assignedRooms);
        model.addAttribute("activePage", "problems");

        return "housekeeper/problems";
    }

    @PostMapping("/problems/report")
    public String reportProblem(
            @RequestParam("branchId") Integer branchId,
            @RequestParam("roomNo") String roomNo,
            @RequestParam("issueDescription") String issueDescription,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isHousekeeper(session)) {
            return "redirect:/login.html";
        }

        String username = getLoggedInUsername();
        try {
            if (issueDescription == null || issueDescription.trim().isEmpty()) {
                throw new IllegalArgumentException("Issue description cannot be empty.");
            }
            if (branchId == null || roomNo == null || roomNo.trim().isEmpty()) {
                throw new IllegalArgumentException("Please select a room.");
            }
            housekeeperService.reportRoomProblem(username, branchId, roomNo, issueDescription);
            redirectAttributes.addFlashAttribute("successMessage", "Problem reported successfully.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error reporting problem: " + e.getMessage());
        }

        return "redirect:/housekeeper/problems";
    }
}
