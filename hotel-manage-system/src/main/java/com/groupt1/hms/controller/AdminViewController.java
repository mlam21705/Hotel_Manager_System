package com.groupt1.hms.controller;

import com.groupt1.hms.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.groupt1.hms.entity.enums.ReportStatus;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminViewController {

    private final AdminService adminService;

    @GetMapping("/home")
    public String adminHome(Model model) {
        model.addAttribute("activePage", "home");
        return "admin/admin-home";
    }

    @GetMapping("/reports")
    public String viewReports(Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("customerReports", adminService.getCustomerReports());
        model.addAttribute("staffReports", adminService.getStaffReports());
        return "admin/reports";
    }

    @GetMapping("/reports/{id}")
    public String viewReportDetail(@PathVariable("id") Integer id, Model model) {
        model.addAttribute("activePage", "reports");
        model.addAttribute("report", adminService.getReportById(id));
        model.addAttribute("statuses", ReportStatus.values());
        return "admin/report-detail";
    }

    @PostMapping("/reports/update-status")
    public String updateReportStatus(@RequestParam("id") Integer id, @RequestParam("status") ReportStatus status, RedirectAttributes redirectAttributes) {
        try {
            adminService.updateReportStatus(id, status);
            redirectAttributes.addFlashAttribute("successMessage", "Report status updated successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to update report status: " + e.getMessage());
        }
        return "redirect:/admin/reports";
    }
}
