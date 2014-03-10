/**
 * Copyright 2014, CITYTECH, Inc.
 * All rights reserved - Do Not Redistribute
 * Confidential and Proprietary
 */
package com.citytechinc.cq.clientlibs.core.services.clientlibs.compilers.less.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import com.citytechinc.cq.clientlibs.api.services.clientlibs.compilers.less.LessCompiler;
import com.citytechinc.cq.clientlibs.api.services.clientlibs.compilers.less.exceptions.LessCompilationException;
import com.google.common.base.Optional;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.mozilla.javascript.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(label="Rhino LESS CSS Compiler", description="")
@Service
@Properties( {
    @Property(name = org.osgi.framework.Constants.SERVICE_VENDOR, value = "CITYTECH, Inc.") } )
public class RhinoLessCompiler implements LessCompiler {

    private static final Logger LOG = LoggerFactory.getLogger(RhinoLessCompiler.class);

    private static final String LESS_RESOURCE_PATH = "/SCRIPT-LIBS/less-rhino-1.6.2.js";

    @Override
    public String compile(String source) throws LessCompilationException {

        Context rhinoContext = Context.enter();

        ScriptableObject standardScope = rhinoContext.initStandardObjects();

        Script lessScript = getLessScript(rhinoContext, LESS_RESOURCE_PATH);

        lessScript.exec(rhinoContext, standardScope);

        standardScope.put("lessSource", standardScope, sanitizeSourceString(source));

        String lessCall =   "var parser = new less.Parser; " +
                            "var result = ''; " +
                            "var lesserror; " +
                            "parser.parse( lessSource, function( error, tree ) { " +
                            "  if ( error ) {" +
                            "    lesserror = error;" +
                            "  }" +
                            "  else {" +
                            "    try { " +
                            "      result = tree.toCSS();" +
                            "    } catch ( e ) {" +
                            "      lesserror = e; " +
                            "    }" +
                            "  }" +
                            "} );";

        rhinoContext.evaluateString(standardScope, lessCall, "generated.js", 1, null);

        Object result = standardScope.get("result", standardScope);
        Object lesserror = standardScope.get("lesserror", standardScope);

        if (!(lesserror instanceof Undefined || lesserror == UniqueTag.NOT_FOUND)) {
            String lessErrorString = lessErrorToString(lesserror);
            LOG.error("A LESS compilation error was encountered : " + lessErrorString);
            LOG.debug(sanitizeSourceString(source));
            throw new LessCompilationException("A LESS compilation error was encountered : " + lessErrorString);
        }
        if (result instanceof Undefined) {
            LOG.error("An undefined result was the product of the LESS compilation.");
            throw new LessCompilationException("An undefined result was the product of the LESS compilation.");
        }

        String resultCss = result.toString();

        return resultCss;
    }

    private String sanitizeSourceString(String source) {
        return source.replace("'", "\'");
    }

    private Script getLessScript(Context context, String path) throws LessCompilationException {

        InputStream scriptStream = getClass().getResourceAsStream(path);

        if (scriptStream == null) {
            return null;
        }

        Reader scriptReader = new InputStreamReader(scriptStream);

        try {
            return context.compileReader(scriptReader, path, 0, null);
        } catch (IOException e) {
            LOG.error("IO Exception hit requesting less compiler script from project resources", e);
            throw new LessCompilationException("IO Exception hit requesting less compiler script from project resources", e);
        } finally {
            IOUtils.closeQuietly(scriptStream);
        }

    }

    /**
     *
     * Properties found in a less error
     *
     * <ul>
     *     <li>line</li>
     *     <li>message</li>
     *     <li>callLine</li>
     *     <li>stack</li>
     *     <li>type</li>
     *     <li>index</li>
     *     <li>extract</li>
     *     <li>callExtract</li>
     *     <li>column</li>
     * </ul>
     * @param lessError
     * @return
     */
    private static String lessErrorToString(Object lessError) {
        NativeObject lessErrorObject = (NativeObject) lessError;

        StringBuffer returnedErrorStringBuffer = new StringBuffer();

        Optional<String> errorLineOptional = getLessErrorStringProperty("line", lessErrorObject);
        Optional<String> errorColumnOptional = getLessErrorStringProperty("column", lessErrorObject);
        Optional<String> errorMessageOptional = getLessErrorStringProperty("message", lessErrorObject);
        Optional<String> errorTypeOptional = getLessErrorStringProperty("type", lessErrorObject);
        Optional<NativeArray> errorExtractOptional = getLessErrorArrayProperty("extract", lessErrorObject);

        returnedErrorStringBuffer.append("Less Compilation Error ");

        if (errorTypeOptional.isPresent()) {
            returnedErrorStringBuffer.append("of type ").append(errorTypeOptional.get()).append(" ");
        }

        returnedErrorStringBuffer.append("encountered ");

        if (errorLineOptional.isPresent()) {
            returnedErrorStringBuffer.append("at line ").append(errorLineOptional.get()).append(" ");
        }
        if (errorColumnOptional.isPresent()) {
            returnedErrorStringBuffer.append("at column ").append(errorColumnOptional.get()).append(" ");
        }
        if (errorMessageOptional.isPresent()) {
            returnedErrorStringBuffer.append(" - ").append(errorMessageOptional.get());
        }
        if (errorExtractOptional.isPresent()) {
            returnedErrorStringBuffer.append("\n");

            for (Object o : errorExtractOptional.get().getIds()) {
                int index = (Integer) o;
                returnedErrorStringBuffer.append(index).append(": ").append(errorExtractOptional.get().get(index, lessErrorObject)).append( "\n" );
            }
        }

        return returnedErrorStringBuffer.toString();

    }

    private static Optional<String> getLessErrorStringProperty(String name, NativeObject lessError) {

        Object lessErrorPropertyValue = lessError.get(name, lessError);

        if (lessErrorPropertyValue != null && !(lessErrorPropertyValue instanceof Undefined)) {
            return Optional.of(lessErrorPropertyValue.toString());
        }

        return Optional.absent();

    }

    private static Optional<NativeArray> getLessErrorArrayProperty(String name, NativeObject lessError) {

        Object lessErrorPropertyValue = lessError.get(name, lessError);

        if (lessErrorPropertyValue != null && !(lessErrorPropertyValue instanceof Undefined)) {
            return Optional.of((NativeArray) lessErrorPropertyValue);
        }

        return Optional.absent();

    }

}
