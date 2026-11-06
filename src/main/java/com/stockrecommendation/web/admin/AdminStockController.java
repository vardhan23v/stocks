package com.stockrecommendation.web.admin;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.stockrecommendation.model.Stock;
import com.stockrecommendation.security.AppUser;
import com.stockrecommendation.service.StockService;

/** Stock records: create, edit, (de)activate, delete. */
@Controller
public class AdminStockController {

    private final StockService stockService;

    public AdminStockController(StockService stockService) {
        this.stockService = stockService;
    }

    /** /admin/stocks, /admin/stocks?new, /admin/stocks?edit=5 */
    @GetMapping("/admin/stocks")
    public String stocks(@RequestParam(required = false) String q, @RequestParam(required = false, name = "new") String create,
                         @RequestParam(required = false) Integer edit, Model model) {
        model.addAttribute("q", q);
        model.addAttribute("stocks", stockService.findAllForAdmin(q));
        if (!model.containsAttribute("form")) {
            if (edit != null) {
                Stock stock = stockService.getStock(edit);
                if (stock != null) {
                    model.addAttribute("form", stock);
                }
            } else if (create != null) {
                model.addAttribute("form", new Stock());
            }
        }
        return "admin/stocks";
    }

    @PostMapping("/admin/stocks")
    public String save(@AuthenticationPrincipal AppUser principal, @ModelAttribute("form") Stock form,
                       @RequestParam(defaultValue = "false") boolean active, RedirectAttributes redirect, Model model) {
        form.setActive(active);
        try {
            if (form.getId() > 0) {
                stockService.update(principal.getUser(), form);
                redirect.addFlashAttribute("success", form.getSymbol() + " was updated.");
            } else {
                stockService.create(principal.getUser(), form);
                redirect.addFlashAttribute("success", form.getSymbol() + " was added. It has no price data until a sync runs.");
            }
            return "redirect:/admin/stocks";
        } catch (IllegalArgumentException | IllegalStateException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("stocks", stockService.findAllForAdmin(null));
            return "admin/stocks";
        }
    }

    @PostMapping("/admin/stocks/{id}/active")
    public String active(@AuthenticationPrincipal AppUser principal, @PathVariable int id, @RequestParam boolean active,
                         RedirectAttributes redirect) {
        try {
            stockService.setActive(principal.getUser(), id, active);
            redirect.addFlashAttribute("success", active ? "The stock was activated." : "The stock was deactivated and is hidden from users.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stocks";
    }

    @PostMapping("/admin/stocks/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUser principal, @PathVariable int id, RedirectAttributes redirect) {
        try {
            stockService.delete(principal.getUser(), id);
            redirect.addFlashAttribute("success", "The stock and all its stored data were deleted.");
        } catch (IllegalStateException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/stocks";
    }
}
