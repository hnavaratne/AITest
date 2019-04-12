AJS.toInit(function ($) {
 var el, newPoint, newPlace, offset;

$('.slider').on("change mousedrag", function() {
     $('#'+$(this).attr('customid')).val($(this).val());
});

 $(".slider").change(function() {

   el = $(this);
   width = el.width();
   newPoint = (el.val() - el.attr("min")) / (el.attr("max") - el.attr("min"));
   offset = -1.3;
   if (newPoint < 0) { newPlace = 0; }
   else if (newPoint > 1) { newPlace = width; }
   else { newPlace = width * newPoint + offset; offset -= newPoint; }
   console.log(el.val());
   if(el.val()==0||el.val()==100){
         el
           .next("#range-slider-value")
            .css({
                     left: newPlace,
                     marginLeft: offset + "%",
                     visibility: 'hidden'
             })
             .text(el.val());
       console.log("trigger");
    }else{
       el
           .next("#range-slider-value")
           .css({
             left: newPlace,
             marginLeft: offset + "%",
             visibility: 'visible'
           })
           .text(el.val());
    }
 })
 .trigger('change');

})(jQuery);