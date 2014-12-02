<%@ Page Language="C#" Inherits="System.Web.Mvc.ViewPage" %>
<div>
    <input type='text' id='my_input'/>
</div>

<script>
    
function $Init(){
    this.find('#my_input').val('Text inserted in Init function');
};

</script>