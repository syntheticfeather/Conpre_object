document.querySelector("button").addEventListener("click", function () {
    alert("Button clicked!");
});


let trs = document.querySelectorAll(".main-content-table tr");
for (let i = 0; i < trs.length; i++) {
    trs[i].addEventListener("mouseover", function () {
        this.style.backgroundColor = '#f2e2e2';
    });
    trs[i].addEventListener("mouseout", function () {
        this.style.backgroundColor = 'white';
    });
}            
