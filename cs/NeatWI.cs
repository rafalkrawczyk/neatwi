/*!
 * NeatWI 1.0
 *
 * http://www.neatwi.com
 *
 * Copyright 2014 Rafał Krawczyk
 * Released under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
 */

using System.Globalization;
using System.IO;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading;
using System.Web;

namespace NeatWI
{
    public class ControlsGenerator
    {
        #region Config

        // Directory of controls files
        const string SOURCE_DIRECTORY = "Controls";

        // Destination directory for js files
        const string DESTINATION_DIRECTORY = "Scripts";

        // Controls function names prefix
        const string PREFIX = "$";

        // If use controls categories
        const bool CATEGORIES = false;

        // New line sign in js files
        const string NEW_LINE = "\r\n";

        #endregion

        #region Language support - for user implementation

        /// <summary>
        /// If you use language support you have to implement this function
        /// </summary>
        /// <param name="language">Language to load</param>
        private void LoadLanguage(string language)
        {
            // For user implementation
            Thread.CurrentThread.CurrentUICulture = new CultureInfo(language);
            Thread.CurrentThread.CurrentCulture = CultureInfo.CreateSpecificCulture(language);
        }

        /// <summary>
        /// If you use language support you can implement this function
        /// so generator can load current language after generation
        /// </summary>
        /// <returns>Current language</returns>
        private string GetLanguage()
        {
            // For user implementation
            return Thread.CurrentThread.CurrentCulture.Name;
        }

        #endregion

        #region Methods for generation

        /// <summary>
        /// Generates controls without language support
        /// </summary>
        public void Generate()
        {
            Generate(null);
        }

        /// <summary>
        /// Generates controls for specified language
        /// </summary>
        /// <param name="languages">Array of language codes or names. Ex. new string[]{"en", "pl"}</param>
        public void Generate(string[] languages)
        {
            if (languages != null && languages.Length > 0)
            {
                string currentLanguage = GetLanguage();

                foreach (string language in languages)
                    GenerateControls(language);

                if (!string.IsNullOrEmpty(currentLanguage))
                    LoadLanguage(currentLanguage);
            }
            else
                GenerateControls(null);
        }

        #endregion

        #region Private members

        /// <summary>
        /// Generate controls for language
        /// </summary>
        /// <param name="language">Language of controls or empty</param>
        private void GenerateControls(string language)
        {
            if (language != null)
                LoadLanguage(language);

            if (CATEGORIES)
            {
                string[] directories = Directory.GetDirectories(Path.Combine(HttpContext.Current.Request.PhysicalApplicationPath, SOURCE_DIRECTORY));
                foreach (string directory in directories)
                    GenerateDirectory(new DirectoryInfo(directory).Name, language);
            }
            else
                GenerateDirectory(string.Empty, language);
        }

        /// <summary>
        /// Generates controls for direcotry
        /// </summary>
        /// <param name="category">Name of the category</param>
        /// <param name="language">Language controls or empty</param>
        private void GenerateDirectory(string category, string language)
        {
            StringBuilder defineBlock = new StringBuilder();
            StringBuilder controlsBlock = new StringBuilder();
            string categoryFileName = Path.Combine(HttpContext.Current.Request.PhysicalApplicationPath, DESTINATION_DIRECTORY) + "/neatwi_" + (!string.IsNullOrEmpty(category) ? category : "controls") + (!string.IsNullOrEmpty(language) ? "_" + language : "") + ".js";
            string[] controlPaths = System.IO.Directory.GetFiles(Path.Combine(HttpContext.Current.Request.PhysicalApplicationPath, SOURCE_DIRECTORY, category), "*.aspx");

            controlsBlock.Append("$.extend($.controls, {");
            bool first = true;

            foreach (string controlPath in controlPaths)
            {
                if (first)
                    first = false;
                else
                    controlsBlock.Append(",");

                string controlName = Path.GetFileNameWithoutExtension(new FileInfo(controlPath).Name);

                controlsBlock.Append("\"" + controlName + "\":\"");

                StringWriter sw = new StringWriter();
                HttpContext.Current.Server.Execute(Path.Combine("~/", SOURCE_DIRECTORY, category, controlName + ".aspx"), sw);
                string controlContent = sw.ToString();
                sw.Close();

                Match mainTag = Regex.Match(controlContent, "< *[^>]+>");
                if (mainTag.Success)
                {
                    Match attribute = Regex.Match(mainTag.Groups[0].Value, "[ '\"]class *= *['\"]([^'\"]*)['\"]");
                    if (attribute.Success)
                    {
                        string classes = attribute.Groups[1].Value.Trim();
                        if (classes.Length > 0)
                        {
                            string[] split = classes.Split(' ');
                            if (split[0] != controlName)
                                controlContent = controlContent.Insert(mainTag.Groups[0].Index + attribute.Groups[1].Index, controlName + " ");
                        }
                        else
                            controlContent = controlContent.Insert(mainTag.Groups[0].Index + attribute.Groups[1].Index, controlName);
                    }
                    else
                        controlContent = new Regex("(< *[^ >]+)").Replace(controlContent, "$1 class=\"" + controlName + "\"", 1);

                    mainTag = Regex.Match(controlContent, "< *[^>]+>");
                    attribute = Regex.Match(mainTag.Groups[0].Value, "[ '\"]id *= *['\"]([^'\"]*)['\"]");
                    if (attribute.Success)
                    {
                        if (attribute.Groups[1].Length == 0)
                            controlContent = controlContent.Insert(mainTag.Groups[0].Index + attribute.Groups[1].Index, controlName + "___id");
                    }
                    else
                        controlContent = new Regex("(< *[^ >]+)").Replace(controlContent, "$1 id=\"" + controlName + "___id\"", 1);
                }

                string html;
                Match scriptStart = Regex.Match(controlContent, "< *script[^>]*>");

                if (scriptStart.Success)
                {
                    html = controlContent.Substring(0, scriptStart.Index);
                    int start = scriptStart.Index + scriptStart.Length;
                    int end;

                    Match scriptEnd = Regex.Match(controlContent, "< */ *script[^>]*>");
                    if (scriptEnd.Success)
                        end = scriptEnd.Index;
                    else
                        end = controlContent.Length;

                    string code = controlContent.Substring(start, end - start);

                    MatchCollection functions = Regex.Matches(code, @"function[ \n]+" + Regex.Escape(PREFIX) + @"([a-zA-Z0-9_]+) ?\(([^\\)]*)\)");
                    if (functions.Count > 0)
                    {
                        defineBlock.Append("/* " + controlName + "*/" + NEW_LINE);
                        int functionsCount = functions.Count;

                        for (int i = 0; i < functionsCount; i++)
                        {
                            Match function = functions[i];
                            defineBlock.Append("$.neatwiDefineFunction(\"" + function.Groups[1].Value + "\",");
                            defineBlock.Append("\"" + controlName + "\",");
                            defineBlock.Append("function(" + function.Groups[2].Value + ")");

                            string functionBody;

                            if (i == functionsCount - 1)
                                functionBody = code.Substring(function.Index + function.Length).Trim();
                            else
                            {
                                int functionStart = function.Index + function.Length;
                                functionBody = code.Substring(functionStart, functions[i + 1].Index - functionStart).Trim();
                            }

                            if (functionBody[functionBody.Length - 1] == ';')
                                functionBody = functionBody.Substring(0, functionBody.Length - 1);

                            defineBlock.Append(functionBody + ");" + NEW_LINE);
                        }
                    }

                }
                else
                    html = controlContent;

                controlsBlock.Append(html.Trim().Replace(System.Environment.NewLine, "").Replace("\\", "\\\\").Replace("\"", "\\\"").Replace("'", "\\'") + "\"");
            }

            controlsBlock.Append("});");
            defineBlock.Append(NEW_LINE + NEW_LINE);

            using (StreamWriter outFile = new StreamWriter(categoryFileName))
            {
                outFile.Write("$(document).ready(function() { " + NEW_LINE + NEW_LINE + defineBlock.ToString() + controlsBlock.ToString() + "});");
                outFile.Flush();
                outFile.Close();
            }
        }

        #endregion
    }
}
