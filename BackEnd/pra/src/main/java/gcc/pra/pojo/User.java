package gcc.pra.pojo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class User {

    @NotNull
    Integer id;

    String username;

    @JsonIgnore
    String password;

    @NotEmpty
    @Pattern(regexp = "^\\S{1,10}$")
    String nickname;

    @NotEmpty
    @Email
    String email;

    String userPic;
    LocalDateTime createTime;
    LocalDateTime updateTime;
}
