package com.adobe.printservice.repository;

import com.adobe.printservice.model.RenderTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RenderTemplateRepository extends JpaRepository<RenderTemplate, String> {
}
