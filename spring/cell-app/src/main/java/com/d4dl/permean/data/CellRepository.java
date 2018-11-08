package com.d4dl.permean.data;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

import java.util.List;

/**
 * Created by joshuadeford on 6/6/17.
 */
@RepositoryRestResource
@CrossOrigin
public interface CellRepository extends PagingAndSortingRepository <Cell, String> {

    /**
     * Generates this query:
      SELECT cell0_.id as id1_0_0_
        , vertex2_.id as id1_2_1_
        , cell0_.area as area6_0_0_
        , cell0_.parent_size as parent_s7_0_0_
        , vertex2_.latitude as latitude6_2_1_
        , vertex2_.longitude as longitud7_2_1_
        , vertices1_.cell_id as cell_id1_1_0__
        , vertices1_.vertices_id as vertices2_1_0__
        , vertices1_.sequence as sequence3_0__
     FROM cell cell0_
        LEFT OUTER JOIN cell_vertices vertices1_
           ON cell0_.id=vertices1_.cell_id
        LEFT OUTER JOIN vertex vertex2_
           ON vertices1_.vertices_id=vertex2_.id
        WHERE (vertex2_.latitude BETWEEN 24.29324189 AND 24.29324191)
        AND (vertex2_.longitude BETWEEN 70.84387674 AND 70.84387676);

     * @param topLat
     * @param bottomLat
     * @param eastLng
     * @param westLng
     * @return
     */
    //@EntityGraph(value = "Cell.vertices", type = EntityGraph.EntityGraphType.FETCH)
    List<Cell> findByCenterLatitudeBetweenAndCenterLongitudeBetween(@Param("top") float topLat, @Param("bottom") float bottomLat, @Param("east") float eastLng, @Param("west") float westLng);
}
