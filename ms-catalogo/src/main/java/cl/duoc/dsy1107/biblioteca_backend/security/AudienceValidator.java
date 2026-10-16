package cl.duoc.dsy1107.biblioteca_backend.security;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

public class AudienceValidator implements OAuth2TokenValidator<Jwt> {
    
    private final String audienciaEsperada;

    public AudienceValidator(String audienciaEsperada) {
        this.audienciaEsperada = audienciaEsperada;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        
        if (jwt.getAudience() != null && jwt.getAudience().contains(audienciaEsperada)) {
            return OAuth2TokenValidatorResult.success();
        }

        OAuth2Error error = new OAuth2Error(
            "invalid_token",
            "el token no contiene la audience esperada para esta API",
            null
        );

        return OAuth2TokenValidatorResult.failure(error);

    }

}
