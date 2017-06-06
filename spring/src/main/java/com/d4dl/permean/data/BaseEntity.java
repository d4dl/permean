package com.d4dl.permean.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Type;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 */
@EntityListeners(AuditingEntityListener.class)
@Data
@MappedSuperclass
@Embeddable
public abstract class BaseEntity implements Serializable {

    @Id
    @GeneratedValue(generator = "uuid", strategy = GenerationType.TABLE)
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    public String id;
    private @Version @JsonIgnore Long version;

    @Basic
    private boolean deleted;

    @LastModifiedDate
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    @JsonIgnore
    private ZonedDateTime updated;

    @CreatedDate
    @Type(type = "org.jadira.usertype.dateandtime.threeten.PersistentZonedDateTime")
    @JsonIgnore
    private ZonedDateTime created;

    public BaseEntity() {
    }
}
