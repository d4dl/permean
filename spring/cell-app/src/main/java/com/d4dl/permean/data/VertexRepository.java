package com.d4dl.permean.data;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Created by joshuadeford on 6/6/17.
 */
@RepositoryRestResource
@CrossOrigin
public interface VertexRepository extends PagingAndSortingRepository <Vertex, String>{
}
