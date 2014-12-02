<?php

/*!
 * NeatWI 1.0
 *
 * http://www.neatwi.com
 *
 * Copyright 2014 Rafal Krawczyk
 * Released under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
 */

class NeatWI {
    
    // Source directory of controls php files
    const SOURCE_DIRECTORY = "controls";
    
    // Destination directory for js files
    const DESTINATION_DIRECTORY = "js";
    
    // Controls function names prefix
    const PREFIX = "$";
    
    // If use controls categories
    const CATEGORIES = TRUE;
    
    // New line sign in js files
    const NEW_LINE = '
';
    
    /**
     * If you use language support you have to implement this function.
     * @param string $language Language to load
     */
    private function load_language($language)
    {
        // For user implementation
    }
    
    /**
     * If you use language support you can implement this function
     * so generator can load current language after generation.
     */
    private function get_language()
    {
        // For user implementation
    }
    
    /**
     * Generate controls.
     * @param array $languages If you use language support specify languages you
     * want to generate controls for. Pass array of languages names/codes.
     * Example array('en', 'pl', 'es')
     */
    public function generate($languages = NULL)
    {
        if ($languages)
        {
            $lang = $this->get_language();
            foreach ($languages as $language)
                $this->generate_controls($language);
            if ($lang)
                $this->load_language($lang);
        }
        else
            $this->generate_controls();
    }
    
    private function generate_controls($language = NULL)
    {
        if ($language)
            $this->load_language ($language);
        
        if (self::CATEGORIES)
        {
            foreach(glob(self::SOURCE_DIRECTORY . "/*", GLOB_ONLYDIR) as $directory)
            {
                $directory_name = preg_split("#/#", $directory);
                $this->generate_directory($directory_name[1], $language);
            }
        }
        else
            $this->generate_directory(NULL, $language);
    }
    
    private function generate_directory($directory, $language)
    {
        $file_name = self::DESTINATION_DIRECTORY . '/neatwi_' . ($directory ? $directory : 'controls') . ($language ? '_' . $language : '') . '.js';
        $define_block = $controls_block = '';
        $controls_block .= '$.extend($.controls, {';
        $first = true;

        foreach(glob(self::SOURCE_DIRECTORY . ($directory ? '/' . $directory : '') . '/*.php') as $control)
        {
            if ($first)
                $first = false;
            else
                $controls_block .= ',';

            $control_name = basename($control, '.php');
            $controls_block .=  "\"" . $control_name . "\":\"";
            
            ob_start();
            include $control;
            $control_content = ob_get_clean();
            
            $script_start = array();
            $script_end = array();
            $functions_matches = array();
            
            $main_tag = array();
            if (preg_match('/< *[^>]+>/', $control_content, $main_tag, PREG_OFFSET_CAPTURE))
            {
                $attribute = array();
                if (preg_match('/[ \'"]class *= *[\'"]([^\'"]*)[\'"]/', $main_tag[0][0], $attribute, PREG_OFFSET_CAPTURE))
                {
                    $class = trim($attribute[1][0]);

                    if (sizeof($class) > 0)
                    {
                        $split = preg_split('/ /', $class);
                        if ($split[0] != $control_name)
                            $control_content = substr_replace($control_content, $control_name . ' ', $main_tag[0][1] + $attribute[1][1], 0);
                    }
                    else
                        $control_content = substr_replace($control_content, $control_name, $main_tag[0][1] + $attribute[1][1], 0);
                }
                else
                    $control_content = preg_replace('/(< *[^ >]+)/', '$1 class="' . $control_name . '"', $control_content, 1);

                preg_match('/< *[^>]+>/', $control_content, $main_tag, PREG_OFFSET_CAPTURE);
                if (preg_match('/[ \'"]id *= *[\'"]([^\'"]*)[\'"]/', $main_tag[0][0], $attribute, PREG_OFFSET_CAPTURE))
                {
                    if (sizeof($attribute[1][0]) == 0)
                        $control_content = substr_replace($control_content, $control_name . '___id', $main_tag[0][1] + $attribute[1][1], 0);
                }
                else
                    $control_content = preg_replace('/(< *[^ >]+)/', '$1 id="' . $control_name . '___id"', $control_content, 1);
            }
            
            if (preg_match('/< *script[^>]*>/', $control_content, $script_start, PREG_OFFSET_CAPTURE))
            {
                $html = substr($control_content, 0, $script_start[0][1]);
                
                if (preg_match('/< *\/ *script[^>]*>/', $control_content, $script_end, PREG_OFFSET_CAPTURE))
                    $end = $script_end[0][1];
                else
                    $end = strlen($control_content);
                
                $start = $script_start[0][1] + strlen($script_start[0][0]);
                $code = substr($control_content, $start, $end - $start);

                if (preg_match_all('/function[ \n]+' . preg_quote(self::PREFIX) . '([a-zA-Z0-9_]+) ?\(([^\)]*)\)/', $code, $functions_matches, PREG_OFFSET_CAPTURE))
                {
                    $define_block .= '/* ' . $control_name . '*/' . self::NEW_LINE;
                    $functions_count = sizeof($functions_matches[0]);

                    for($i = 0; $i < $functions_count; $i++)
                    {
                        $define_block .= '$.neatwiDefineFunction("' . $functions_matches[1][$i][0] . '",';
                        $define_block .= '"' . $control_name . '",';
                        $define_block .= 'function(' . $functions_matches[2][$i][0] . ')';

                        if ($i == $functions_count - 1)
                            $function_body = trim(substr($code, $functions_matches[0][$i][1] + strlen($functions_matches[0][$i][0])));
                        else
                        {
                            $fun_start = $functions_matches[0][$i][1] + strlen($functions_matches[0][$i][0]);
                            $function_body = trim(substr($code, $fun_start, $functions_matches[0][$i+1][1] - $fun_start));
                        }

                        if ($function_body[strlen($function_body)-1] == ';')
                                $function_body = substr($function_body, 0, strlen($function_body)-1);
                        $define_block .= $function_body . ');' . self::NEW_LINE;
                    }
                }
            }
            else
                $html = $control_content;
            
            $controls_block .= addslashes($html) . "\"";
        }
        
        $controls_block .= "});";
        $controls_block = preg_replace('/\s\s+/', '', $controls_block);
        $define_block .= self::NEW_LINE . self::NEW_LINE;

        file_put_contents($file_name, '$(document).ready(function() { ' . self::NEW_LINE . self::NEW_LINE . $define_block . $controls_block . '});');
    }
}

?>
