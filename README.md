NeatWi
======

NeatWI is solution to make building web interfaces easier.

website: <a href="http://www.neatwi.com">neatwi.com</a>

Author
======

<a href="github.com/rafalkrawczyk">Rafał Krawczyk</a>

<a href="mainto:rafalkrawczyk88@gmail.com">rafalkrawczyk88@gmail.com

Example control
======

  ```html
  <div>
    <input type='text' id='id'/>
  </div>

  <script>

  function $Init(){
    this.find('#id').val('Example text');
  }

  function $MyFunction(){
    alert(this.find('#id').val());
  }

  </script>
  ```
  
Usage
======

  ```js
  $('#container').neatwiLoad('my_control');
  $('.my_control').MyFunction();
  ```
  
License
======

<a href="http://www.opensource.org/licenses/mit-license.php">MIT</a>
