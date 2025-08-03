package gcc.pra.pojo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.groups.Default;
import lombok.Data;

@Data
public class Category {

    @NotNull(groups = update.class)
    private Integer id;
    @NotEmpty
    private String categoryName;
    @NotEmpty
    private String categoryAlias;
    private Integer createUser;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
 
    public interface add extends Default {

    }

    public interface update extends Default {

    }
}
