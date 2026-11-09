const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
class Element {
 constructor() {this.children=[];this.dataset={};this.classList={toggle(){}};this.listeners={};}
 append(...items){this.children.push(...items)}
 replaceChildren(...items){this.children=items}
 setAttribute(){} removeAttribute(){}
 addEventListener(name,fn){this.listeners[name]=fn}
}
const elements = new Map();
const document = {title:'', querySelector(s){if(!elements.has(s))elements.set(s,new Element());return elements.get(s)}, querySelectorAll(){return []},createElement(){return new Element()}};
const source = fs.readFileSync('src/main/resources/static/assets/app.js','utf8');
const context = vm.createContext({document,Intl,Date,Number,String,Option:class {constructor(text,value){this.text=text;this.value=value}}, state:{view:'vehicles', user:{id:1,driver:true,role:'MEMBER'},vehicleMonth:'2096-02',vehicleId:'1',vehicles:[{id:1,name:'Car'}],bookings:[{id:1,vehicleId:1,userId:2,username:'Other',date:'2096-02-01',slot:'MORNING'}]}});
vm.runInContext('const dayNames=["ראשון","שני","שלישי","רביעי","חמישי","שישי","שבת"];const $=s=>document.querySelector(s);'+source.slice(source.indexOf('function isAdmin()'),source.indexOf('function errorMessage('))+source.slice(source.indexOf('function textElement('),source.indexOf('async function loadData('))+source.slice(source.indexOf('function renderVehicles()'),source.indexOf('$("#vehicleSelect").addEventListener')),context);
vm.runInContext('renderVehicles()',context);
const days = elements.get('#vehicleCalendar').children.filter(e=>e.children.length===4);
assert.equal(days.length,29,'leap February has 29 days');
assert.equal(days[0].children[1].disabled,true,'another driver cannot cancel');
assert.equal(days[0].children[2].disabled,false,'free slot is available');
assert.equal(elements.get('#primaryAction').hidden,true,'member cannot add vehicles');
context.state.user.role='ADMIN';vm.runInContext('renderVehicles()',context);
assert.equal(elements.get('#primaryAction').hidden,false);
assert.equal(elements.get('#vehicleCalendar').children.filter(e=>e.children.length===4)[0].children[1].disabled,false,'admin can cancel');
console.log('Calendar UI checks passed');
