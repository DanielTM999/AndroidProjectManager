package dtm.core.apptest;

import dtm.dependencymanager.core.ApplicationCore;
import dtm.dependencymanager.annotations.UseExceptionHandler;

@UseExceptionHandler(ExceptionHandlerTeste.class)
public class Startup extends ApplicationCore {

}
