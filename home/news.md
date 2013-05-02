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
    <h1 class="newstitle">
      {{ post.title }}
    </h1>
  </a>
  <div>
    {{ post.date | date: "%d.%m.%Y." }}, poster: {{ post.poster }}
  </div>
  {{ post.content }}
  {% endfor %}
</div>






