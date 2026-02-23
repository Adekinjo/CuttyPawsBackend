package com.cuttypaws.repository;

import com.cuttypaws.entity.PostMedia;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepo extends JpaRepository<PostMedia, Long> {
}
