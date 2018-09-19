package com.d4dl.permean.data;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 */
@EntityListeners(AuditingEntityListener.class)
@Data
@MappedSuperclass
@Embeddable
public abstract class BasicEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid", strategy = GenerationType.TABLE)
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    public String id;

    public BasicEntity() {
    }
}
