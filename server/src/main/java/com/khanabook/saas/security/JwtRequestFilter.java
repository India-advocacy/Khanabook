package com.khanabook.saas.security;

import com.khanabook.saas.debug.DebugNDJSONLogger;
import com.khanabook.saas.utility.JwtUtility;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

	private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

	private final JwtUtility jwtUtility;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {

		final String authorizationHeader = request.getHeader("Authorization");
		final String path = request.getRequestURI();

		if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
			String jwt = authorizationHeader.substring(7);

			boolean tokenExpired = true;
			boolean jwtExtractOk = false;
			Long restaurantIdPresent = null;
			boolean tenantSet = false;

			try {
				tokenExpired = jwtUtility.isTokenExpired(jwt);
				if (!tokenExpired) {
					Long restaurantId = jwtUtility.extractRestaurantId(jwt);
					String username = jwtUtility.extractUsername(jwt);
					jwtExtractOk = true;
					restaurantIdPresent = restaurantId;

					if (restaurantId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

						TenantContext.setCurrentTenant(restaurantId);
						tenantSet = true;

						UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
								username, null, Collections.emptyList());
						authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
						SecurityContextHolder.getContext().setAuthentication(authToken);
					}
				}
			} catch (Exception e) {

				logger.debug("JWT validation failed: " + e.getClass().getSimpleName());
			}

			DebugNDJSONLogger.log(
					"pre-debug",
					"H1_JWT_MISSING_OR_INVALID",
					"JwtRequestFilter:doFilterInternal",
					"JWT auth header inspected",
					java.util.Map.of(
							"path", path,
							"authorizationHeaderPresent", true,
							"tokenExpired", tokenExpired,
							"jwtExtractOk", jwtExtractOk,
							"restaurantIdPresent", restaurantIdPresent != null,
							"tenantSet", tenantSet
					)
			);
		}
		else {
			DebugNDJSONLogger.log(
					"pre-debug",
					"H1_JWT_MISSING_OR_INVALID",
					"JwtRequestFilter:doFilterInternal",
					"JWT auth header missing or not Bearer",
					java.util.Map.of(
							"path", path,
							"authorizationHeaderPresent", false
					)
			);
		}

		try {
			chain.doFilter(request, response);
		} finally {

			TenantContext.clear();
			SecurityContextHolder.clearContext();
		}
	}
}
