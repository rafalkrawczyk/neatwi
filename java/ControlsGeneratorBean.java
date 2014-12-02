/*!
 * NeatWI 1.0
 *
 * http://www.neatwi.com
 *
 * Copyright 2014 Rafa³ Krawczyk
 * Released under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
 */

package neatwi;

import javax.faces.bean.ManagedBean;
import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.bean.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@ManagedBean(name = "ControlsGeneratorBean")
@SessionScoped
public class ControlsGeneratorBean {
    
    // Full path to your project
    static final String PROJECT_DIRECTORY = "C:/SetThisPath";
    
    // Path to source controls files f.e. "web"
    static final String FILES_SUBDIRECTORY = "web";
    
    // URL fragment for pages f.e. "faces" for JSF
    static final String PAGES_SUBDIRECTORY = "faces";
    
    // Source directory of controls files
    static final String SOURCE_DIRECTORY = "controls";
    
    // Destination directory for js files
    static final String DESTINATION_DIRECTORY = "js";
    
    // Controls function names prefix
    static final String PREFIX = "$";
    
    // If use controls categories
    static final Boolean CATEGORIES = true;
    
    // New line sign in js files
    static final String NEW_LINE = "\n";
    
    // If you use language support specify base language files package
    static final String LANGUAGE_RESOUCE_PACKAGE = "main.";
    
    // If you use language support specify array of resource files names
    // without language specificator and extension
    static final String[] LANGUAGE_RESOURCES = {"language"};
    
    // If you use langue support specify array of langauges for which controls
    // will be generated
    static final String[] LANGUAGES = {"pl", "en"};

    
    private String _sessionId;
    private String _baseURL;
    private HashMap<String, ResourceBundle> _resources;
    
    /**
     * You can change this function if you need different language support
     * @param language Language name
     */
    private void loadLanguage(String language)
    {
        Locale locale = new Locale(language);
        _resources = new HashMap<String, ResourceBundle>();
        for(String resource : LANGUAGE_RESOURCES)
            _resources.put(resource, ResourceBundle.getBundle(LANGUAGE_RESOUCE_PACKAGE + resource, locale));
    }
    
    /**
     * If you use build in language support you can use this function in
     * your controls to get messages from resources
     * @param resource Resource name
     * @param line Value name
     * @return Line from reesource file
     */
    public String getLine(String resource, String line)
    {
        ResourceBundle resourceBundle = _resources.get(resource);
        return resourceBundle == null ? "RESOURCE DOES NOT EXIST" : resourceBundle.getString(line);
    }
    
    /**
     * If you use language support you can implement this function
     * so generator can load current language after generation.
     */
    private String getLanguage()
    {
        // For user implementation
        return "";
    }
    
    /**
     * Generates controls
     */
    public void generate()
    {
        try
        {
            ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
            _sessionId = ((HttpSession) externalContext.getSession(false)).getId();
            HttpServletRequest request = (HttpServletRequest)externalContext.getRequest();
            String url = ((HttpServletRequest)externalContext.getRequest()).getRequestURL().toString();
            _baseURL = url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath() + "/";

            if (LANGUAGES != null && LANGUAGES.length > 0)
            {
                String currentLanguage = getLanguage();

                for (String language : LANGUAGES)
                    generateControls(language);

                if (!currentLanguage.isEmpty())
                    loadLanguage(currentLanguage);
            }
            else
                generateControls(null);
        }
        catch (Exception exception)
        {
            System.out.printf("Sory but some error accured: " + exception.getMessage());
        }
    }
    
    private File combine(final String... paths){
        if(paths == null || paths.length == 0) { 
            return null; 
        } 

        File file = new File(paths[0]); 
        final int len = paths.length; 
        for (int i = 1; i < len; i++) { 
            if (paths[i] != null && !paths[i].isEmpty())
                file = new File(file, paths[i]); 
        }

        return file;
    }
    
    private String combineURL(final String... paths) throws MalformedURLException{
        if (paths == null || paths.length == 0)
            return "";
        
        int i = 0;
        while(i < paths.length && (paths[i] == null || paths[i].isEmpty()))
            i++;
        
        if (i >= paths.length)
            return "";
        
        String result = paths[i];
        
        for(i++; i < paths.length; i++)
        {
            if (paths[i] == null || paths[i].isEmpty())
                continue;
            
            if (result.substring(result.length() - 1).compareTo("/") != 0)
                result += "/";
            
            result += paths[i].substring(0, 1).compareTo("/") == 0 ? paths[i].substring(paths[i].length()-1) : paths[i];
        }
        
        return result;
    }
    
    private String getFileName(File file)
    {
        String fileName = file.getName();
        int index = fileName.lastIndexOf(".");
        
        if (index != -1)
            return fileName.substring(0, index);
        return fileName;
    }
    
    private ArrayList<MatchResult> getMatches(String content, String pattern)
    {
        Matcher matcher = Pattern.compile(pattern).matcher(content);
        ArrayList<MatchResult> result = new ArrayList<MatchResult>();

        while(matcher.find())
            result.add(new MatchResult(matcher));
        
        return result;
    }
    
    private void generateControls(String language) throws IOException, MalformedURLException, ServletException
    {
        if (language != null)
            loadLanguage(language);
        
        if (CATEGORIES)
        {
            String[] directories = combine(PROJECT_DIRECTORY, FILES_SUBDIRECTORY, SOURCE_DIRECTORY).list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });
            
            for(String category : directories)
                generateDirectory(category, language);
        }
        else
            generateDirectory(null, language);
    }
    
    private String readBuffer(BufferedReader reader) throws IOException
    {
        StringBuilder everything = new StringBuilder();
        String line;
        while( (line = reader.readLine()) != null) {
           everything.append(line).append(NEW_LINE);
        }
        return everything.toString();
    }
    
    private String insertString(String original, String insertString, int position)
    {
        StringBuilder buffer = new StringBuilder(original);
        buffer.insert(position, insertString);
        return buffer.toString();
    }
    
    private void generateDirectory(String category, String language) throws MalformedURLException, IOException, ServletException
    {
        String categoryFileName = combine(PROJECT_DIRECTORY, FILES_SUBDIRECTORY, DESTINATION_DIRECTORY) + "/neatwi_" + (category != null ? category : "controls") + (language != null ? "_" + language : "") + ".js";
        String content;
        
        try
        {
            StringBuilder defineBlock = new StringBuilder();
            StringBuilder controlsBlock = new StringBuilder();

            File folder = combine(PROJECT_DIRECTORY, FILES_SUBDIRECTORY, SOURCE_DIRECTORY, category);
            File[] controlPaths = folder.listFiles();

            controlsBlock.append("$.extend($.controls, {");
            boolean first = true;

            for(File controlFile : controlPaths)
            {
                if (first)
                    first = false;
                else
                    controlsBlock.append(",");

                String controlName = getFileName(controlFile);

                controlsBlock.append("\"").append(controlName).append("\":\"");

                BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(combineURL(_baseURL, PAGES_SUBDIRECTORY, SOURCE_DIRECTORY, category, controlFile.getName() + ";jsessionid=" + _sessionId)).openStream()));
                String controlContent = readBuffer(reader);
                reader.close();
                
                Matcher mainTag = Pattern.compile("< *[^>]+>").matcher(controlContent);
                if (mainTag.find())
                {
                    Matcher attribute = Pattern.compile("[ '\"]class *= *['\"]([^'\"]*)['\"]").matcher(mainTag.group());
                    if (attribute.find())
                    {
                        String classes = attribute.group(1).trim();
                        if (classes.length() > 0)
                        {
                            String[] split = classes.split(" ");
                            if (!split[0].equals(controlName))
                                controlContent = insertString(controlContent, controlName + " ", mainTag.start() + attribute.start(1));
                        }
                        else
                            controlContent = insertString(controlContent, controlName, mainTag.start() + attribute.start(1));
                    }
                    else
                        controlContent = controlContent.replaceFirst("(< *[^ >]+)", "$1 class=\"" + controlName + "\"");
                    
                    mainTag = Pattern.compile("< *[^>]+>").matcher(controlContent);
                    mainTag.find();
                    attribute = Pattern.compile("[ '\"]id *= *['\"]([^'\"]*)['\"]").matcher(mainTag.group());
                    if (attribute.find())
                    {
                        if (attribute.end(1) != attribute.start(1))
                            controlContent = insertString(controlContent, controlName + "___id", mainTag.start() + attribute.start(1));
                    }
                    else
                        controlContent = controlContent.replaceFirst("(< *[^ >]+)", "$1 id=\"" + controlName + "___id\"");
                }

                String html;
                Matcher scriptStart = Pattern.compile("< *script[^>]*>").matcher(controlContent);

                if (scriptStart.find())
                {
                    html = controlContent.substring(0, scriptStart.start());
                    int start = scriptStart.end();
                    int end;

                    Matcher scriptEnd = Pattern.compile("< */ *script[^>]*>").matcher(controlContent);
                    if (scriptEnd.find())
                        end = scriptEnd.start();
                    else
                        end = controlContent.length();

                    String code = controlContent.substring(start, end);

                    ArrayList<MatchResult> functions = getMatches(code, "function[ \\n\\r]+" + Pattern.quote(PREFIX) + "([a-zA-Z0-9_]+) *\\(([^\\)]*)\\)");
                    int functionsCount = functions.size();
                    if (functionsCount > 0)
                    {
                        defineBlock.append("/* ").append(controlName).append("*/" + NEW_LINE);

                        for(int i = 0; i < functionsCount; i++)
                        {
                            MatchResult function = functions.get(i);
                            defineBlock.append("$.neatwiDefineFunction(\"").append(function.getGroup(1)).append("\",");
                            defineBlock.append("\"").append(controlName).append("\",");
                            defineBlock.append("function(").append(function.getGroup(2)).append(")");

                            String functionBody;

                            if (i == functionsCount - 1)
                                functionBody = code.substring(function.getIndex() + function.getLength()).trim();
                            else
                                functionBody = code.substring(function.getIndex() + function.getLength(), functions.get(i + 1).getIndex()).trim();

                            if (functionBody.substring(functionBody.length() - 1).equals(";"))
                                functionBody = functionBody.substring(0, functionBody.length() - 1);

                            defineBlock.append(functionBody).append(");" + NEW_LINE);
                        }
                    }
                }
                else
                    html = controlContent;

                controlsBlock.append(html.trim().replace(NEW_LINE, "").replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")).append("\"");
            }

            controlsBlock.append("});");
            defineBlock.append(NEW_LINE).append(NEW_LINE);
            content = "$(document).ready(function() { " + NEW_LINE + NEW_LINE + defineBlock.toString() + controlsBlock.toString() + "});";
        }
        catch(IOException exception)
        {
            content = "alert('Sory but some error accured: " + exception.getMessage() + "');";
        }

        PrintWriter writer = new PrintWriter(categoryFileName);
        writer.print(content);
        writer.close();
    }
}

class MatchResult
{
    private final String[] _groups;
    private final int _index;
    private final int _length;
    
    public MatchResult(Matcher matcher)
    {
        int groupsCount = matcher.groupCount();
        _groups = new String[groupsCount+1];
        for(int i = 0; i <= groupsCount; i++)
            _groups[i] = matcher.group(i);
        
        _index = matcher.start();
        _length = matcher.end() - _index;
    }
    
    public int getIndex()
    {
        return _index;
    }
    
    public int getLength()
    {
        return _length;
    }
    
    public String getGroup(int index)
    {
        return index <= _groups.length ? _groups[index] : "";
    }
}