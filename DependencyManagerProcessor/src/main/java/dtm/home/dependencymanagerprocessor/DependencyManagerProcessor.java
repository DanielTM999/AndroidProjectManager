package dtm.home.dependencymanagerprocessor;

import com.google.auto.service.AutoService;
import java.io.Writer;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
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

    private static final String COMPONENT_FQN = "dtm.dependencymanager.annotations.Component";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (generated || roundEnvironment.processingOver()) return true;

        printLog("DependencyManagerProcessor executando");
        generated = true;
        Set<? extends Element> services = findServices(roundEnvironment);
        generateAutoloader(services);

        return true;
    }

    private Set<? extends Element> findServices(RoundEnvironment roundEnv) {
        Set<Element> result = new HashSet<>();

        for (Element element : roundEnv.getRootElements()) {
            if (element.getKind() != ElementKind.CLASS) continue;

            if (isComponent(element)) {
                result.add(element);
            }
        }

        return result;
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
                            "       dependencyContainer.registerDependency(%s.class);\n",
                            qualifiedName
                    )
            );
        }
        return sb.toString();
    }

    private String getTemplateClass(){
        return """
        package dtm.core.dependencymanager.generated;
        
        import dtm.dependencymanager.internal.Autoloader;
        import dtm.dependencymanager.core.DependencyContainer;
        
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

    private boolean isComponent(Element element) {
        for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
            TypeElement annotationType = (TypeElement) annotation.getAnnotationType().asElement();

            if (hasComponentMeta(annotationType, new HashSet<>())) {
                return true;
            }
        }
        return false;
    }

    private boolean hasComponentMeta(TypeElement annotation, Set<String> visited) {
        String name = annotation.getQualifiedName().toString();

        if (!visited.add(name)) return false;

        if (name.equals(COMPONENT_FQN)) {
            return true;
        }

        for (AnnotationMirror meta : annotation.getAnnotationMirrors()) {
            TypeElement metaType = (TypeElement) meta.getAnnotationType().asElement();

            if (hasComponentMeta(metaType, visited)) {
                return true;
            }
        }
        
        return false;
    }


    public void printLog(String message){
        printLog(message, Diagnostic.Kind.NOTE);
    }

    public void printLog(String message, Diagnostic.Kind kind){
        processingEnv.getMessager().printMessage(
                kind,
                message
        );
    }

}