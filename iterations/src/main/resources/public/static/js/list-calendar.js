(()=>{"use strict";var e={5767:(e,t,n)=>{var r=n(7294),o=n(3935),a=n(1219),i=n(1106),l=n(2318),c=n(3957),u=n(3758),f=n(2822),s=n(1120),d=n(1749),m=(n(4713),n(2871));function p(e,t){return function(e){if(Array.isArray(e))return e}(e)||function(e,t){if("undefined"!=typeof Symbol&&Symbol.iterator in Object(e)){var n=[],r=!0,o=!1,a=void 0;try{for(var i,l=e[Symbol.iterator]();!(r=(i=l.next()).done)&&(n.push(i.value),!t||n.length!==t);r=!0);}catch(e){o=!0,a=e}finally{try{r||null==l.return||l.return()}finally{if(o)throw a}}return n}}(e,t)||y(e,t)||function(){throw new TypeError("Invalid attempt to destructure non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}()}function y(e,t){if(e){if("string"==typeof e)return h(e,t);var n=Object.prototype.toString.call(e).slice(8,-1);return"Object"===n&&e.constructor&&(n=e.constructor.name),"Map"===n||"Set"===n?Array.from(e):"Arguments"===n||/^(?:Ui|I)nt(?:8|16|32)(?:Clamped)?Array$/.test(n)?h(e,t):void 0}}function h(e,t){(null==t||t>e.length)&&(t=e.length);for(var n=0,r=new Array(t);n<t;n++)r[n]=e[n];return r}var v=(0,s.Z)((function(){return{root:{"& > *":{margin:30}},center:{display:"flex",alignItems:"center",justifyContent:"center",width:"90%"}}})),b=function(){var e=v(),t=p((0,r.useState)([]),2),n=t[0],o=t[1],s=p((0,r.useState)(-1),2),b=s[0],g=s[1];(0,r.useEffect)((function(){fetch("/api/calendars",{method:"GET",mode:"cors"}).then((function(e){return e.json()})).then((function(e){var t;console.log(e),o(function(e){if(Array.isArray(e))return h(e)}(t=e)||function(e){if("undefined"!=typeof Symbol&&Symbol.iterator in Object(e))return Array.from(e)}(t)||y(t)||function(){throw new TypeError("Invalid attempt to spread non-iterable instance.\nIn order to be iterable, non-array objects must have a [Symbol.iterator]() method.")}())}))}),[]),(0,r.useEffect)((function(){b>0&&(fetch("/api/delcalendar?id="+b,{method:"POST",mode:"cors"}).then((function(e){location.reload()})),g(-1))}),[b]);var E=function(e){return function(){g(e)}},S=n.map((function(e){return{id:e.id,content:e.title}})),w=function(e){return function(){window.location.assign("/view-calendar?id="+e)}};return r.createElement("div",{style:{paddingBottom:"100px"}},r.createElement(i.Z,null),r.createElement(d.Z,{container:!0,spacing:3},r.createElement(d.Z,{item:!0,xs:6},r.createElement("div",{className:e.root},r.createElement(l.Z,{variant:"h4",className:"headingTyp"},"Calendar List"),r.createElement("div",{className:"divContents"},r.createElement(f.Z,null,S.map((function(e){return r.createElement(a.Z,{delete:E,key:e.id,id:e.id,content:e.content,clicked:w})})))),r.createElement("div",{className:e.center},r.createElement(u.Z,{color:"primary","aria-label":"add",onClick:function(){window.location.assign("/create-calendar")}},r.createElement(c.Z,null))))),r.createElement(d.Z,{container:!0,spacing:3},r.createElement(m.Z,null))))};o.render(r.createElement(b,null),document.getElementById("root"))}},t={};function n(r){if(t[r])return t[r].exports;var o=t[r]={exports:{}};return e[r](o,o.exports,n),o.exports}n.m=e,n.n=e=>{var t=e&&e.__esModule?()=>e.default:()=>e;return n.d(t,{a:t}),t},n.d=(e,t)=>{for(var r in t)n.o(t,r)&&!n.o(e,r)&&Object.defineProperty(e,r,{enumerable:!0,get:t[r]})},n.o=(e,t)=>Object.prototype.hasOwnProperty.call(e,t),n.r=e=>{"undefined"!=typeof Symbol&&Symbol.toStringTag&&Object.defineProperty(e,Symbol.toStringTag,{value:"Module"}),Object.defineProperty(e,"__esModule",{value:!0})},n.j=176,(()=>{var e={176:0},t=[[5767,851,638,643,715,749,434,471,35,779,878,106,871]],r=()=>{};function o(){for(var r,o=0;o<t.length;o++){for(var a=t[o],i=!0,l=1;l<a.length;l++){var c=a[l];0!==e[c]&&(i=!1)}i&&(t.splice(o--,1),r=n(n.s=a[0]))}return 0===t.length&&(n.x(),n.x=()=>{}),r}n.x=()=>{n.x=()=>{},i=i.slice();for(var e=0;e<i.length;e++)a(i[e]);return(r=o)()};var a=o=>{for(var a,i,[c,u,f,s]=o,d=0,m=[];d<c.length;d++)i=c[d],n.o(e,i)&&e[i]&&m.push(e[i][0]),e[i]=0;for(a in u)n.o(u,a)&&(n.m[a]=u[a]);for(f&&f(n),l(o);m.length;)m.shift()();return s&&t.push.apply(t,s),r()},i=self.webpackChunkplenum=self.webpackChunkplenum||[],l=i.push.bind(i);i.push=a})(),n.x()})();
//# sourceMappingURL=list-calendar.js.map