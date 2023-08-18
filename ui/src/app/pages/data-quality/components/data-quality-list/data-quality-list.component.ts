import { Component, OnInit } from '@angular/core';
import { DataQuailtyListResponse } from '../../models/data-quality-list-response';
import { ActivatedRoute, Router } from '@angular/router';

@Component({
  selector: 'app-data-quality-list',
  templateUrl: './data-quality-list.component.html',
  styleUrls: ['./data-quality-list.component.scss']
})
export class DataQualityListComponent implements OnInit {

  public dataQualityList: Array<DataQuailtyListResponse> = [];

  private projectId: number;

  constructor(
    private readonly router: Router,
    private readonly activatedRoute: ActivatedRoute) {
    this.projectId = +this.activatedRoute.snapshot.paramMap.get('projectId');
  }

  ngOnInit(): void {

    const dataQuality1 = {
      id: 1,
      ruleName: 'DQ Name1',
      ruleType: 'Data completeness-Row',
      ruleDescription: 'This is added test rules',
      createdBy: 'Naveen',
      createdDate: new Date(),
      modifiedDate: new Date(),
      status: 'Active'

    } as DataQuailtyListResponse;

    this.dataQualityList.push(dataQuality1);
  }

  public edit(dataQuality: DataQuailtyListResponse): void {
    sessionStorage.setItem('editDataQuality', JSON.stringify(dataQuality));
    this.router.navigate([`projects/${this.projectId}/data-quality/edit`]);
  }
}
