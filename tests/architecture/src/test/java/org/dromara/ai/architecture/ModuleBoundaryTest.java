package org.dromara.ai.architecture;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Tag;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "org.dromara.ai")
@Tag("dev")
public class ModuleBoundaryTest {

    @ArchTest
    static final ArchRule infrastructureIsNotAccessedOutsideInfrastructureLayer = noClasses()
            .that().resideOutsideOfPackages("..infrastructure..")
            .should().accessClassesThat().resideInAnyPackage("..infrastructure..")
            .because("AI 模块必须通过公共契约通信，不能直接访问 Mapper、Entity 或数据库基础设施");
}
