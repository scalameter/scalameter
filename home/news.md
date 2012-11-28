---
layout: default
title: News
permalink: /news/index.html
---




<ul class="entries">
  {% for post in site.posts %}
  <a href="/scalameter/{{ post.url }}">
    <li class="newstitle">
      <span>{{ post.title }}</span>
      <br/>
      {{ post.date | date: "%d.%m.%Y." }}
    </li>
  </a>
  {% endfor %}
</ul>






