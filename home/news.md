---
layout: default
title: News
permalink: /news/index.html
---




<div class="newsentries">
  {% for post in site.posts %}
  <a href="/scalameter/{{ post.url }}">
    <br/>
    <br/>
    <div>
    <h1 class="newstitle">
      {{ post.title }}
    </h1>
      {{ post.date | date: "%d.%m.%Y." }}
    </div>
  </a>
  {{ post.content }}
  {% endfor %}
</div>






