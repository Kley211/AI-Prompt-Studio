package org.dromara.ai.architecture;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Tag("dev")
public class ModuleBoundaryTest {

    @Test
    void projectInfrastructureIsPrivateToProjectModule() {
        noClasses()
            .that().resideOutsideOfPackage("org.dromara.ai.project..")
            .should().accessClassesThat().resideInAnyPackage("org.dromara.ai.project.infrastructure..")
            .because("其他 AI 模块必须通过 ai-project 公共服务访问项目数据，不能直接访问 Mapper")
            .check(new ClassFileImporter().importPackages("org.dromara.ai"));
    }
}
