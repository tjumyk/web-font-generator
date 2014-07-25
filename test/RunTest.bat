@ echo off
echo // > assets\css\custom_font.css
del assets\fonts /s /q
java -jar ..\dist\webfontgen.jar *.ttf -o assets\fonts -i *.html,*htm -x assets,bin -c assets\css\custom_font.css -h -m -d
start test.html
