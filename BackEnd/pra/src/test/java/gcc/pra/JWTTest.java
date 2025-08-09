package gcc.pra;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

public class JWTTest {

    @Test
    public void test() {
        Map<String, Object> c = new HashMap<>();
        c.put("name", "John");
        c.put("age", 30);

        String token = JWT.create()
                .withClaim("user", c)
                .withExpiresAt(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 3))
                .sign(Algorithm.HMAC256("gcc"));

        System.out.println(token);
    }

    @Test
    public void testParse() {
        JWTVerifier verifier = JWT.require(Algorithm.HMAC256("gcc")).build();
        DecodedJWT jwt = verifier.verify("eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE3NTQwNTEyNDMsInVzZXIiOnsibmFtZSI6IkpvaG4iLCJhZ2UiOjMwfX0.kS5ohidOLX7ZE1AztU4rfG2so9AqpTSc89HiPpoyqKM");
        Map<String, Claim> claims = jwt.getClaims();
        System.out.println(claims);
    }
}
