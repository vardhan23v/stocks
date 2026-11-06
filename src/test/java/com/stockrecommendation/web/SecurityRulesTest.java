package com.stockrecommendation.web;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.stockrecommendation.config.SecurityConfig;
import com.stockrecommendation.service.AdminService;
import com.stockrecommendation.service.AuthService;
import com.stockrecommendation.service.MarketDataService;
import com.stockrecommendation.service.StockService;
import com.stockrecommendation.web.admin.AdminDashboardController;
import com.stockrecommendation.web.admin.AdminUserController;

/** URL security from SecurityConfig, checked without a database. */
@WebMvcTest(controllers = {AdminDashboardController.class, AdminUserController.class})
@Import(SecurityConfig.class)
class SecurityRulesTest {

    @Autowired
    private MockMvc mvc;

    @MockBean private AuthService authService;
    @MockBean private AdminService adminService;
    @MockBean private StockService stockService;
    @MockBean private MarketDataService marketDataService;

    @Test
    void anonymousVisitorIsSentToLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void regularUserCannotOpenAdminPages() throws Exception {
        mvc.perform(get("/admin/users").with(user("priya@stocksense.test").roles("USER")))
           .andExpect(redirectedUrl("/dashboard?denied"));
    }

    @Test
    void regularUserCannotPostAdminActions() throws Exception {
        mvc.perform(post("/admin/users/2/delete").with(csrf()).with(user("priya@stocksense.test").roles("USER")))
           .andExpect(redirectedUrl("/dashboard?denied"));
    }

    @Test
    void postWithoutCsrfTokenIsRejected() throws Exception {
        mvc.perform(post("/admin/users/2/delete").with(user("admin@stocksense.test").roles("ADMIN")))
           .andExpect(status().isForbidden());
    }
}
