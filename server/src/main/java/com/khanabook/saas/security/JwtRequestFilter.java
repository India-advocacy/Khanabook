package com.khanabook.saas.security;

import com.khanabook.saas.utility.JwtUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

	private final JwtUtility jwtUtility;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		final String authorizationHeader = request.getHeader("Authorization");

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			String jwt = authorizationHeader.substring(7);
			try {
				if (!jwtUtility.isTokenExpired(jwt)) {
					Long restaurantId = jwtUtility.extractRestaurantId(jwt);
					String username = jwtUtility.extractUsername(jwt);

					if (restaurantId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

						TenantContext.setCurrentTenant(restaurantId);

						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
								username, null, Collections.emptyList());
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
					}
				}
			} catch (Exception e) {

				logger.debug("JWT validation failed: " + e.getClass().getSimpleName());
			}
		}

		try {
			chain.doFilter(request, response);
		} finally {

			TenantContext.clear();
			SecurityContextHolder.clearContext();
		}
	}
}
