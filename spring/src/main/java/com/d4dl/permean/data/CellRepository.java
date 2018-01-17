package com.d4dl.permean.data;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

import java.math.BigDecimal;
import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
@RepositoryRestResource
public interface CellRepository extends PagingAndSortingRepository <Cell, String> {

    @EntityGraph(value = "Cell.vertices", type = EntityGraph.EntityGraphType.LOAD)
    List<Cell> findByVerticesLatitudeBetweenAndVerticesLongitudeBetween(@Param("top") BigDecimal topLat, @Param("bottom") BigDecimal bottomLat, @Param("east") BigDecimal eastLng, @Param("west") BigDecimal westLng);
}
