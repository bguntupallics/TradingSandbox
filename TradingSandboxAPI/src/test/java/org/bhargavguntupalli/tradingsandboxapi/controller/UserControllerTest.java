package org.bhargavguntupalli.tradingsandboxapi.controller;

import org.bhargavguntupalli.tradingsandboxapi.controllers.UserController;
import org.bhargavguntupalli.tradingsandboxapi.dto.UserDto;
import org.bhargavguntupalli.tradingsandboxapi.security.CustomUserDetailsService;
import org.bhargavguntupalli.tradingsandboxapi.security.JwtProvider;
import org.bhargavguntupalli.tradingsandboxapi.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // ── Existing tests ──────────────────────────────────────────────────

    @Test
    void getBalance_ShouldReturnUserBalance() throws Exception {
        String username = "johndoe";
        BigDecimal balance = new BigDecimal("1234.56");
        UserDto dto = new UserDto();
        dto.setCashBalance(balance);

        when(userService.getUserDtoByUsername(username)).thenReturn(dto);

        mockMvc.perform(get("/api/account/balance")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(balance.toString()));
    }

    @Test
    void getBalance_ShouldReturnUserTheme() throws Exception {
        String username = "johndoe";
        UserDto dto = new UserDto();
        dto.setThemePreference("LIGHT");

        when(userService.getUserDtoByUsername(username)).thenReturn(dto);

        mockMvc.perform(get("/api/account/theme")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("LIGHT"));
    }

    @Test
    void updateProfile_ShouldReturnUpdatedUserDto() throws Exception {
        String username = "johndoe";

        String payload = """
            {
              "username":"johndoe",
              "email":"new@example.com",
              "firstName":"New",
              "lastName":"Name",
              "themePreference":"DARK"
            }
            """;

        UserDto returnedDto = new UserDto();
        returnedDto.setEmail("new@example.com");
        returnedDto.setFirstName("New");
        returnedDto.setLastName("Name");
        returnedDto.setThemePreference("DARK");

        when(userService.updateProfile(eq(username), Mockito.any(UserDto.class)))
                .thenReturn(returnedDto);

        MvcResult result = mockMvc.perform(post("/api/account/update")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andReturn();

        System.out.println("Payload was: " + payload);
        System.out.println("Response: " + result.getResponse().getContentAsString());
        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        mockMvc.perform(post("/api/account/update")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.firstName").value("New"))
                .andExpect(jsonPath("$.lastName").value("Name"))
                .andExpect(jsonPath("$.themePreference").value("DARK"));
    }

    // ── New tests ───────────────────────────────────────────────────────

    @Test
    void getProfile_Returns200() throws Exception {
        String username = "johndoe";
        UserDto dto = new UserDto();
        dto.setId(1L);
        dto.setUsername("johndoe");
        dto.setEmail("john@example.com");
        dto.setFirstName("John");
        dto.setLastName("Doe");
        dto.setThemePreference("LIGHT");
        dto.setCashBalance(new BigDecimal("10000.00"));

        when(userService.getUserDtoByUsername(username)).thenReturn(dto);

        mockMvc.perform(get("/api/account")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("johndoe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.themePreference").value("LIGHT"))
                .andExpect(jsonPath("$.cashBalance").value(10000.00));
    }

    @Test
    void creditBalance_Returns200() throws Exception {
        String username = "johndoe";
        String payload = """
            {
              "amount": 500.00
            }
            """;

        UserDto returnedDto = new UserDto();
        returnedDto.setUsername("johndoe");
        returnedDto.setCashBalance(new BigDecimal("10500.00"));

        when(userService.creditBalance(eq(username), any(BigDecimal.class)))
                .thenReturn(returnedDto);

        mockMvc.perform(post("/api/account/update/balance/credit")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(10500.00))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void debitBalance_Returns200() throws Exception {
        String username = "johndoe";
        String payload = """
            {
              "amount": 250.00
            }
            """;

        UserDto returnedDto = new UserDto();
        returnedDto.setUsername("johndoe");
        returnedDto.setCashBalance(new BigDecimal("9750.00"));

        when(userService.debitBalance(eq(username), any(BigDecimal.class)))
                .thenReturn(returnedDto);

        mockMvc.perform(post("/api/account/update/balance/debit")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(9750.00))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }

    @Test
    void changeTheme_Returns200() throws Exception {
        String username = "johndoe";

        UserDto returnedDto = new UserDto();
        returnedDto.setUsername("johndoe");
        returnedDto.setThemePreference("DARK");

        when(userService.toggleTheme(username)).thenReturn(returnedDto);

        mockMvc.perform(post("/api/account/change-theme")
                        .principal(new UsernamePasswordAuthenticationToken(username, ""))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.themePreference").value("DARK"))
                .andExpect(jsonPath("$.username").value("johndoe"));
    }
}
