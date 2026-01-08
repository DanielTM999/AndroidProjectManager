package dtm.home.dependencymanagerprocessor;

import com.google.auto.service.AutoService;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@AutoService(Processor.class)
public class DependencyManagerProcessor extends AbstractProcessor {
    private boolean generated = false;

    private static final String COMPONENT_FQN = "dtm.dependencymanager.annotations.Component";
    private static final String RESOURCE_FILE = "META-INF/dtm/autoloader.name";
    private static final String PACKAGE_NAME = "dtm.dependencymanager.generated";

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        if (generated || roundEnvironment.processingOver()) return true;

        printLog("DependencyManagerProcessor executando");
        Set<? extends Element> services = findServices(roundEnvironment);

        if(!services.isEmpty()){
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String simpleClassName = "DependencyManagerAutoloader_" + uuid;
            String fullClassName = PACKAGE_NAME + "." + simpleClassName;
            generateAutoloader(services, simpleClassName);
            generateResourcePointer(fullClassName);
            generated = true;
        }

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

    private void generateAutoloader(Set<? extends Element> services, String simpleClassName) {
        try {
            JavaFileObject file = createSourceFile(simpleClassName);

            String baseTemplateClass = getTemplateClass();
            String bodyLoadMethod = buildLoadBody(services);
            String content = String.format(
                    baseTemplateClass,
                    PACKAGE_NAME,
                    simpleClassName,
                    bodyLoadMethod
            );

            try (Writer writer = file.openWriter()) {
                writer.write(content);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void generateResourcePointer(String fullClassName) {
        try {
            FileObject file = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT,
                    "",
                    RESOURCE_FILE
            );

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8))) {
                writer.write(fullClassName);
            }

            printLog("Resource criado apontando para: " + fullClassName);

        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar resource pointer: " + e.getMessage(), e);
        }
    }

    private JavaFileObject createSourceFile(String simpleClassName) throws Exception {
        return processingEnv.getFiler()
                .createSourceFile(PACKAGE_NAME + "." + simpleClassName);
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
        package %1$s;
        
        import dtm.dependencymanager.internal.Autoloader;
        import dtm.dependencymanager.core.DependencyContainer;
        
        public final class %2$s implements Autoloader {
        
            private static final %2$s INSTANCE = new %2$s();
        
            private %2$s() {
            }
        
            public static %2$s getInstance() {
                return INSTANCE;
            }
        
            @Override
            public void load(DependencyContainer dependencyContainer) {
               try {
               %3$s
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