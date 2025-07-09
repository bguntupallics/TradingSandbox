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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)   // ← disables the JWT/security filter
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private JwtProvider jwtProvider;
    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

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

        // JSON payload for the POST /update call
        String payload = """
            {
              "username":"johndoe",
              "email":"new@example.com",
              "firstName":"New",
              "lastName":"Name",
              "themePreference":"DARK"
            }
            """;

        // Prepare the DTO the service should return
        UserDto returnedDto = new UserDto();
        returnedDto.setEmail("new@example.com");
        returnedDto.setFirstName("New");
        returnedDto.setLastName("Name");
        returnedDto.setThemePreference("DARK");

        // Mock the updateProfile call
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
}
