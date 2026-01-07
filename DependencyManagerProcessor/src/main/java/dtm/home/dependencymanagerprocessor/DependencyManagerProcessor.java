package dtm.home.dependencymanagerprocessor;

import com.google.auto.service.AutoService;

import java.io.Writer;
import java.util.Set;
import dtm.dependencymanager.annotations.Service;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class DependencyManagerProcessor extends AbstractProcessor {
    private boolean generated = false;

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (generated || roundEnvironment.processingOver()) return true;

        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.NOTE,
                "DependencyManagerProcessor executando"
        );

        generated = true;
        Set<? extends Element> services = findServices(roundEnvironment);
        generateAutoloader(services);

        return true;
    }

    private Set<? extends Element> findServices(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(Service.class);
    }

    private void generateAutoloader(Set<? extends Element> services) {
        try {
            JavaFileObject file = createSourceFile();

            String baseTemplateClass = getTemplateClass();
            String bodyLoadMethod = buildLoadBody(services);
            String content = String.format(baseTemplateClass, bodyLoadMethod);

            try (Writer writer = file.openWriter()) {
                writer.write(content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private JavaFileObject createSourceFile() throws Exception {
        return processingEnv.getFiler()
                .createSourceFile("dtm.core.dependencymanager.generated.DependencyManagerAutoloader");
    }

    private String buildLoadBody(Set<? extends Element> services)  {
        StringBuilder sb = new StringBuilder();
        for (Element element : services) {
            if (element.getKind() != ElementKind.CLASS) continue;

            TypeElement type = (TypeElement) element;
            String qualifiedName = type.getQualifiedName().toString();

            sb.append(
                    String.format(
                            "            dependencyContainer.registerDependency(%s.class);\n",
                            qualifiedName
                    )
            );
        }
        return sb.toString();
    }

    private String getTemplateClass(){
        return """
        package dtm.core.dependencymanager.generated;
        
        import dtm.core.dependencymanager.internal.Autoloader;
        import dtm.core.dependencymanager.core.DependencyContainer;
        
        public final class DependencyManagerAutoloader implements Autoloader {
        
            private static final DependencyManagerAutoloader INSTANCE = new DependencyManagerAutoloader();
        
            private DependencyManagerAutoloader() {
            }
        
            public static DependencyManagerAutoloader getInstance() {
                return INSTANCE;
            }
        
            @Override
            public void load(DependencyContainer dependencyContainer) {
               try {
                    %s
               } catch (Exception e) {
                    throw new RuntimeException(e);
               }
            }
        }
        """;
    }

}