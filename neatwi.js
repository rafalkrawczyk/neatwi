/*!
 * NeatWI 1.0
 *
 * http://www.neatwi.com
 *
 * Copyright 2014 Rafal‚ Krawczyk
 * Released under the MIT license:
 *   http://www.opensource.org/licenses/mit-license.php
 */

$(document).ready(function(){
    
    function getFirstClass(classTag){
        return classTag.indexOf(" ") == -1 ? classTag : classTag.split(" ")[0];
    }
    
    $.controls = {};
    $.controlFunctions = {};
    
    $.neatwiGetControl = function(controlName, controlId){
        var control = $.controls[controlName];
        
        if (!control){
            console.error('Control ' + controlName + ' is not present in available controls collection.');
            return '';
        }
        
        return control.replace(/___id/g, typeof(controlId) == 'undefined' ? '' : controlId);
    };
    
    $.fn.neatwiLoad = function(controlName, controlId, loadData, setData, mode){
        var control = $($.neatwiGetControl(controlName, controlId));
        
        if (!mode || mode == 'append')
            $(this).append(control);
        else if (mode == 'prepend')
            $(this).prepend(control);
        else if (mode == 'after')
            $(this).after(control);
        else if (mode == 'before')
            $(this).before(control);
        else
            $(this).html(control);
        
        var initFunction = $.controlFunctions['Init'][controlName];
        if (initFunction)
            initFunction.call(control); 
        
        if (loadData != null)
            control.Load(loadData);
        
        if (setData != null)
            control.SetData(setData);
        
        return control;
    };

    $.neatwiDefineFunction = function(functionName, controlName, functionDefinition){
        if (typeof($.controlFunctions[functionName]) == 'undefined')
            $.controlFunctions[functionName] = {};
        
        $.controlFunctions[functionName][controlName] = functionDefinition;
        
        if (typeof($.fn[functionName]) == 'undefined'){
            $.fn[functionName] = function(){
                var calledArguments = arguments;
                
                if ($(this).length > 1){
                    var result = true;
                    $(this).each(function(){
                        var functionToCall = $.controlFunctions[functionName][getFirstClass($(this).attr('class'))];
                        if (!functionToCall)
                            console.error(functionName + ' method called but control does not have this method defined. Control class: ' + $(this).attr('class'));
                        else
                            result &= functionToCall.apply($(this), calledArguments); 
                    });
                    return result;
                }
                else if($(this).length > 0){
                    var functionToCall = $.controlFunctions[functionName][getFirstClass($(this).attr('class'))];
                    if (!functionToCall)
                        console.error(functionName + ' method called but control does not have this method defined. Control class: ' + $(this).attr('class'));
                    else
                        return functionToCall.apply($(this), calledArguments); 
                }
            };
        }
    };
});