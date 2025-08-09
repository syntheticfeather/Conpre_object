package gcc.pra.Utils;

import java.util.Date;
import java.util.Map;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

public class JWTutil {

    final static String secret = "secret";

    public static String generateToken(Map<String, Object> claims) {
        String token = JWT.create()
                .withClaim("Claims", claims)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 24))
                .sign(Algorithm.HMAC256(secret));
        return token;
    }

    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(secret))
                .build()
                .verify(token)
                .getClaim("Claims")
                .asMap();
    }
}
