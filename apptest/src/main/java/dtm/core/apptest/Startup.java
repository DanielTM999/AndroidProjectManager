package dtm.core.apptest;

import dtm.core.dependencymanager.annotations.UseExceptionHandler;
import dtm.core.dependencymanager.core.ApplicationCore;

@UseExceptionHandler(ExceptionHandlerTeste.class)
public class Startup extends ApplicationCore {}
