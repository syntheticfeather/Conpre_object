package gcc.pra.pojo;

import java.util.List;

import lombok.Data;

@Data
public class PageBean<T> {

    private Long total;
    private List<T> items;

}
