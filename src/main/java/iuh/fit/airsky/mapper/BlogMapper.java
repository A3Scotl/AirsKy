package iuh.fit.airsky.mapper;

import iuh.fit.airsky.dto.request.BlogRequest;
import iuh.fit.airsky.dto.response.BlogResponse;
import iuh.fit.airsky.model.Blog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BlogMapper {
    
    @Mapping(target = "blogId", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "categories", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "viewCount", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "slug", ignore = true) // Will be generated from title
    Blog toEntity(BlogRequest dto);

    BlogResponse toResponseDTO(Blog entity);
}
