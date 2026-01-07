package dtm.core.apptest;

import dtm.core.dependencymanager.core.ApplicationCore;
import dtm.dependencymanager.annotations.UseExceptionHandler;

@UseExceptionHandler(ExceptionHandlerTeste.class)
public class Startup extends ApplicationCore {

}
